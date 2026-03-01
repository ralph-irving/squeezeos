DESCRIPTION = "Open source VPN daemon"
HOMEPAGE = "https://openvpn.net/"
LICENSE = "GPLv2"
PR = "r2"

DEPENDS = " \
        lzo \
	openssl \
"

RDEPENDS_${PN} += " \
	lzo \
	libssl \
	libcrypto \
"

SRC_URI = " \
	https://swupdate.openvpn.org/community/releases/${PN}-${PV}.tar.gz \
	file://autoconf261.patch;patch=1 \
"

SRC_URI_append_jive = " \
        file://tun.ko-jive \
"

SRC_URI_append_baby = " \
        file://tun.ko-baby \
"

SRC_URI_append_fab4 = " \
        file://tun.ko-fab4 \
"

EXTRA_OECONF = " --with-crypto-library=openssl --disable-plugin-auth-pam --disable-plugin-down-root "

inherit autotools

inherit module-base
MODULES=/lib/modules/${KERNEL_VERSION}

do_configure_append() {
	for i in $(find ${S} -type f \( -name Makefile -o -name Makefile -o -name 'version.sh' -o -name 'config.h' \)); do
		sed -i -e s:2.PRODUCT_VERSION_MINORPRODUCT_VERSION_PATCH:${PV}:g $i
	done
}

do_install_append() {
	install -m 0755 -d ${D}/${layout_sysconfdir}/${PN}
	install -m 0755 -d ${D}/${MODULES}
}

do_install_append_jive() {
	install -m 0644 ${S}/../tun.ko-jive ${D}/${MODULES}/tun.ko
}

do_install_append_baby() {
	install -m 0644 ${S}/../tun.ko-baby ${D}/${MODULES}/tun.ko
}

do_install_append_fab4() {
	install -m 0644 ${S}/../tun.ko-fab4 ${D}/${MODULES}/tun.ko
}

FILES_${PN}_jive = " \
	${layout_sysconfdir}/${PN} \
	${layout_sbindir}/openvpn \
	${MODULES}/tun.ko \
"

FILES_${PN}_baby = " \
	${layout_sysconfdir}/${PN} \
	${layout_sbindir}/openvpn \
	${MODULES}/tun.ko \
"

FILES_${PN}_fab4 += " \
	${layout_sysconfdir}/${PN} \
	${layout_sbindir}/openvpn \
	${MODULES}/tun.ko \
"
