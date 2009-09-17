/*
 * Copyright 2008 Freescale Semiconductor, Inc. All Rights Reserved.
 */

/*
 * The code contained herein is licensed under the GNU General Public
 * License. You may obtain a copy of the GNU General Public License
 * Version 2 or later at the following locations:
 *
 * http://www.opensource.org/licenses/gpl-license.html
 * http://www.gnu.org/copyleft/gpl.html
 */

/*!
 * @file  linux/drivers/input/keyboard/mpr084.c
 *
 * @brief Driver for the Freescale MPR084 I2C Touch Sensor KeyPad module.
 *
 *
 *
 * @ingroup Keypad
 */

#include <linux/module.h>
#include <linux/init.h>
#include <linux/slab.h>
#include <linux/i2c.h>
#include <linux/string.h>
#include <linux/bcd.h>
#include <linux/list.h>
#include <linux/device.h>
#include <linux/interrupt.h>
#include <linux/kthread.h>
#include <linux/input.h>
#include <linux/delay.h>
#include <asm/mach/irq.h>
#include <asm/arch/gpio.h>

/*
 * Definitions
 */
#define DEBUG 0

#define KEY_COUNT         8

/*
  *Registers in MPR084
  */
#define MPR084_FIFO_ADDR                        0x00
#define MPR084_FAULT_ADDR                       0x01
#define MPR084_TPS_ADDR                         0x02
#define MPR084_TPC_ADDR                         0x03
#define MPR084_STR1_ADDR                        0x04
#define MPR084_STR2_ADDR                        0x05
#define MPR084_STR3_ADDR                        0x06
#define MPR084_STR4_ADDR                        0x07
#define MPR084_STR5_ADDR                        0x08
#define MPR084_STR6_ADDR                        0x09
#define MPR084_STR7_ADDR                        0x0A
#define MPR084_STR8_ADDR                        0x0B
#define MPR084_ECEM_ADDR                        0x0C
#define MPR084_MNTP_ADDR                        0x0D
#define MPR084_MTC_ADDR                         0x0E
#define MPR084_TASP_ADDR                        0x0F
#define MPR084_SC_ADDR                          0x10
#define MPR084_LPC_ADDR                         0x11
#define MPR084_SKT_ADDR                         0x12
#define MPR084_CONFIG_ADDR                      0x13
#define MPR084_SI_ADDR                          0x14
#define MPR084_ADDR_MINI                        MPR084_FIFO_ADDR
#define MPR084_ADDR_MAX                         MPR084_SI_ADDR

/* FIFO registers */
#define MPR084_FIFO_MORE_DATA_FLAG              0x80
#define MPR084_FIFO_NO_DATA_FLAG                0x40
#define MPR084_FIFO_OVERFLOW_FLAG               0x20
#define MPR084_FIFO_PAD_IS_TOUCHED              0x10
#define MPR084_FIFO_POSITION_MASK               0x0F

#define DRIVER_NAME "MPR084"

struct mpr084_data {
	struct i2c_client *client;
	struct device_driver driver;
	struct input_dev *idev;
	struct task_struct *tstask;
	struct completion kpirq_completion;
	int kpirq;
	int kp_thread_cnt;
};

static int kpstatus[KEY_COUNT];
static struct mxc_keyp_platform_data *keypad;
static const unsigned short *mxckpd_keycodes;

static int mpr084_read_register(struct mpr084_data *data,
				unsigned char regaddr, int *value)
{
	int ret = 0;
	unsigned char regvalue;

	ret = i2c_master_send(data->client, &regaddr, 1);
	if (ret < 0)
		goto err;
	udelay(20);
	ret = i2c_master_recv(data->client, &regvalue, 1);
	if (ret < 0)
		goto err;
	*value = regvalue;

	return ret;
err:
	return -ENODEV;
}

static irqreturn_t mpr084_keypadirq(int irq, void *v)
{
	struct mpr084_data *d = v;

	disable_irq(d->kpirq);
	complete(&d->kpirq_completion);
	return IRQ_HANDLED;
}

static int mpr084ts_thread(void *v)
{
	struct mpr084_data *d = v;
	int ret = 0, fifo = 0;
	int index = 0, currentstatus = 0;

	if (d->kp_thread_cnt)
		return -EINVAL;
	d->kp_thread_cnt = 1;
	while (1) {

		if (kthread_should_stop())
			break;
		/* Wait for keypad interrupt */
		if (wait_for_completion_interruptible_timeout
		    (&d->kpirq_completion, HZ) <= 0)
			continue;
		ret = mpr084_read_register(d, MPR084_FIFO_ADDR, &fifo);
		if (ret < 0) {
			printk(KERN_ERR
			       "%s: Err in reading keypad FIFO register \n\n",
			       __func__);
		} else {
			if (fifo & MPR084_FIFO_OVERFLOW_FLAG)
				printk(KERN_ERR
				       "%s: FIFO overflow \n\n", __func__);
			while (!(fifo & MPR084_FIFO_NO_DATA_FLAG)) {
				index = fifo & MPR084_FIFO_POSITION_MASK;
				currentstatus =
				    fifo & MPR084_FIFO_PAD_IS_TOUCHED;
				/*Scan key map for changes */
				if ((currentstatus) ^ (kpstatus[index])) {
					if (!(currentstatus)) {
						/*Key released. */
						input_event(d->idev, EV_KEY,
							    mxckpd_keycodes
							    [index], 0);
					} else {
						/* Key pressed. */
						input_event(d->idev, EV_KEY,
							    mxckpd_keycodes
							    [index], 1);
					}
					/*Store current keypad status */
					kpstatus[index] = currentstatus;
				}
				mpr084_read_register(d, MPR084_FIFO_ADDR,
						     &fifo);
				if (fifo & MPR084_FIFO_OVERFLOW_FLAG)
					printk(KERN_ERR
					       "%s: FIFO overflow \n\n",
					       __func__);
			}
		}
		/* Re-enable interrupts */
		enable_irq(d->kpirq);
	}

	d->kp_thread_cnt = 0;
	return 0;
}

static int mpr084_idev_open(struct input_dev *idev)
{
	struct mpr084_data *d = idev->private;
	int ret = 0;

	d->tstask = kthread_run(mpr084ts_thread, d, DRIVER_NAME "kpd");
	if (IS_ERR(d->tstask))
		ret = PTR_ERR(d->tstask);
	return ret;
}

static void mpr084_idev_close(struct input_dev *idev)
{
	struct mpr084_data *d = idev->private;

	if (!IS_ERR(d->tstask))
		kthread_stop(d->tstask);
}

static int mpr084_driver_register(struct mpr084_data *data)
{
	struct input_dev *idev;
	int ret = 0;

	if (data->kpirq) {
		ret =
		    request_irq(data->kpirq, mpr084_keypadirq,
				IRQF_TRIGGER_FALLING, DRIVER_NAME, data);
		if (!ret) {
			init_completion(&data->kpirq_completion);
		} else {
			printk(KERN_ERR "%s: cannot grab irq %d\n",
			       __func__, data->kpirq);
		}

	}
	idev = input_allocate_device();
	data->idev = idev;
	idev->private = data;
	idev->name = DRIVER_NAME;
	idev->open = mpr084_idev_open;
	idev->close = mpr084_idev_close;
	if (!ret)
		ret = input_register_device(idev);

	return ret;
}

static int mpr084_i2c_remove(struct i2c_client *client)
{
	int err;
	struct mpr084_data *d = i2c_get_clientdata(client);

	free_irq(d->kpirq, d);
	input_unregister_device(d->idev);
	if (keypad->inactive)
		keypad->inactive();
	err = i2c_detach_client(client);
	if (err) {
		dev_err(&client->dev, "Client deregistration failed, "
			"client not detached.\n");
		return err;
	}

	return 0;
}

static int mpr084_write_register(struct mpr084_data *data,
				 u8 regaddr, u8 regvalue)
{
	int ret = 0;
	unsigned char msgbuf[2];

	msgbuf[0] = regaddr;
	msgbuf[1] = regvalue;
	ret = i2c_master_send(data->client, msgbuf, 2);
	if (ret < 0) {
		printk(KERN_ERR "%s - Error in writing to I2C Register %d \n",
		       __func__, regaddr);
		return ret;
	}

	return ret;
}
static int mpr084_configure(struct mpr084_data *data)
{
	int ret = 0;

	ret = mpr084_write_register(data, MPR084_TPC_ADDR, 0xbd);
	if (ret < 0)
		goto err;
	ret = mpr084_write_register(data, MPR084_STR1_ADDR, 0x00);
	if (ret < 0)
		goto err;
	ret = mpr084_write_register(data, MPR084_STR2_ADDR, 0x00);
	if (ret < 0)
		goto err;
	ret = mpr084_write_register(data, MPR084_STR3_ADDR, 0x00);
	if (ret < 0)
		goto err;
	ret = mpr084_write_register(data, MPR084_STR4_ADDR, 0x00);
	if (ret < 0)
		goto err;
	ret = mpr084_write_register(data, MPR084_STR5_ADDR, 0x00);
	if (ret < 0)
		goto err;
	ret = mpr084_write_register(data, MPR084_STR6_ADDR, 0x00);
	if (ret < 0)
		goto err;
	ret = mpr084_write_register(data, MPR084_STR7_ADDR, 0x00);
	if (ret < 0)
		goto err;
	ret = mpr084_write_register(data, MPR084_STR8_ADDR, 0x00);
	if (ret < 0)
		goto err;
	/* channel enable mask: enable all */
	ret = mpr084_write_register(data, MPR084_ECEM_ADDR, 0xff);
	if (ret < 0)
		goto err;
	/*two conccurrent touch position allowed */
	ret = mpr084_write_register(data, MPR084_MNTP_ADDR, 0x07);
	if (ret < 0)
		goto err;
	/*Sample period */
	ret = mpr084_write_register(data, MPR084_TASP_ADDR, 0x10);
	if (ret < 0)
		goto err;
	/*enabled IRQEN, RUNE, IRQR */
	ret = mpr084_write_register(data, MPR084_CONFIG_ADDR, 0x17);
	if (ret < 0)
		goto err;
	return ret;
err:
	return -ENODEV;
}

static int mpr084_i2c_probe(struct i2c_client *client)
{
	struct mpr084_data *data;
	int err = 0, i = 0;

	data = kzalloc(sizeof(struct mpr084_data), GFP_KERNEL);
	if (data == NULL)
		return -ENOMEM;
	i2c_set_clientdata(client, data);
	data->client = client;
	data->kpirq = client->irq;
	err = mpr084_driver_register(data);
	if (err < 0)
		goto exit_free;
	keypad = (struct mxc_keyp_platform_data *)(client->dev).platform_data;
	if (keypad->active)
		keypad->active();
	mxckpd_keycodes = keypad->matrix;
	data->idev->keycode = &mxckpd_keycodes;
	data->idev->keycodesize = sizeof(unsigned char);
	data->idev->keycodemax = KEY_COUNT;
	data->idev->id.bustype = BUS_I2C;
	__set_bit(EV_KEY, data->idev->evbit);
	for (i = 0; i < 8; i++)
		__set_bit(mxckpd_keycodes[i], data->idev->keybit);
	err = mpr084_configure(data);
	if (err == -ENODEV) {
		free_irq(data->kpirq, data);
		input_unregister_device(data->idev);
		goto exit_free;
	}
	memset(kpstatus, 0, sizeof(kpstatus));
	printk(KERN_INFO "%s: Device Attached\n", __func__);
	return 0;
exit_free:
	kfree(data);
	return err;
}

static struct i2c_driver mpr084_driver = {
	.driver = {
		   .name = DRIVER_NAME,
		   },
	.probe = mpr084_i2c_probe,
	.remove = mpr084_i2c_remove,
	.command = NULL,
};
static int __init mpr084_init(void)
{
	return i2c_add_driver(&mpr084_driver);
}

static void __exit mpr084_exit(void)
{
	i2c_del_driver(&mpr084_driver);
}

MODULE_AUTHOR("Freescale Semiconductor Inc");
MODULE_DESCRIPTION("MPR084 Touch KeyPad Controller driver");
MODULE_LICENSE("GPL");
module_init(mpr084_init);
module_exit(mpr084_exit);