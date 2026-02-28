DESCRIPTION = "Linux kernel for jive devices"
SECTION = "kernel"
LICENSE = "GPL"

LINUX_VERSION = "2.6.22"
PV = "${LINUX_VERSION}+${DISTRO_VERSION}+svnr${SRCREV}"
PR = "r5"

inherit kernel

SRC_URI = " \
          ${SQUEEZEOS_SVN};module=s3c2412 \
          file://defconfig-jive \
          "

S = "${WORKDIR}/s3c2412/linux-${LINUX_VERSION}"

COMPATIBLE_MACHINE = "(jive)"


# new kernel patches are managed by quilt, and checked into svn.
do_patch() {
	cd ${S}
	quilt push -a
}


do_configure_prepend() {

	rm -f ${S}/.config || true

        if [ "${TARGET_OS}" = "linux-gnueabi" -o  "${TARGET_OS}" = "linux-uclibcgnueabi" ]; then
                echo "CONFIG_AEABI=y"                   >> ${S}/.config
                echo "CONFIG_OABI_COMPAT=y"             >> ${S}/.config
        else
                echo "# CONFIG_AEABI is not set"        >> ${S}/.config
                echo "# CONFIG_OABI_COMPAT is not set"  >> ${S}/.config
        fi

        sed     -e '/CONFIG_AEABI/d' \
                -e '/CONFIG_OABI_COMPAT=/d' \
                '${WORKDIR}/defconfig-${MACHINE}' >>'${S}/.config'

        yes '' | oe_runmake oldconfig

}

# jive needs EXTRAVERSION -P7 for legacy reasons
EXTRA_OEMAKE = "EXTRAVERSION=-P7-gd076382e"

# adjust the package names to remove the -P7
PKG_kernel-image = "kernel-image-${PV}"
PKG_kernel-base = "kernel-${PV}"

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

