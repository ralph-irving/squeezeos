DESCRIPTION = "Linux kernel for Squeezebox baby"
SECTION = "kernel"
LICENSE = "GPL"

LINUX_ARCH = imx25
LINUX_VERSION = "2.6.26"
PV = "${LINUX_VERSION}+${DISTRO_VERSION}+svnr${SRCREV}"
PR = "r11"


inherit kernel

SRC_URI = " \
          ${KERNELORG_MIRROR}pub/linux/kernel/v2.6/linux-2.6.26.tar.bz2 \
          ${SQUEEZEOS_SVN};module=${LINUX_ARCH} \
          "

S = "${WORKDIR}/linux-${LINUX_VERSION}"

COMPATIBLE_MACHINE = "(baby)"


# the kernel patches are managed by quilt, and checked into svn. modify patch
# to simply apply the patchset using quilt.
do_patch() {
	#cp -rf ${WORKDIR}/${LINUX_ARCH}/patches ${S}
	cp -rf /home/ralphy/source/squeezeos/src/imx25/patches ${S}
	cd ${S}
	quilt push -a
}

do_deploy() {
	install -d ${DEPLOY_DIR_IMAGE}
	install -m 0644 arch/${ARCH}/boot/${KERNEL_IMAGETYPE} ${DEPLOY_DIR_IMAGE}/${KERNEL_IMAGETYPE}-${PV}-${MACHINE}-${DATETIME}.bin
	cd ${DEPLOY_DIR_IMAGE}
	ln -sf ${KERNEL_IMAGETYPE}-${PV}-${MACHINE}-${DATETIME}.bin ${KERNEL_IMAGETYPE}-${MACHINE}.bin
	tar -cvzf ${DEPLOY_DIR_IMAGE}/modules-${KERNEL_RELEASE}-${MACHINE}.tgz -C ${D} lib	
}

do_rm_work() {
	# modules use the kernel source, don't remove it
	echo "** DID NOT REMOVE KERNEL SOURCE, MODULES USE THIS **"
}
