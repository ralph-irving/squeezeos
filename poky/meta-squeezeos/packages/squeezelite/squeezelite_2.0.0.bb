SUMMARY = "Lightweight headless squeezebox player for Lyrion Media Server"
LICENSE = "GPLv3"

PR = "r1561"

DEPENDS = " \
	flac \
	libmad \
	tremor2 \
	mpg123 \
	faad2 \
	opusfile \
	libffmpeg \
	openssl \
	soxr \
"

RDEPENDS_${PN} += " \
	libasound \
	libflac \
	libmad \
	tremor2 \
	libmpg123 \
	libfaad \
	libffmpeg \
	libopusfile \
	libssl \
	libcrypto \
	soxr \
"

SRC_URI="${RALPHY_SQUEEZEOS}/${PN}-${PV}-${PR}.tar.gz \
	file://squeezelite-squeezeos.patch;patch=1 \
	file://load-libtremor-first.patch;patch=1 \
	file://tremor-oob.patch;patch=1 \
	file://updateconfig \
	file://initd \
	file://Makefile.squeezeos \
	file://SetupSqueezelite \
"

SRC_URI_append_baby = " \
	file://slimproto-baby.patch;patch=1 \
	file://config-baby \
	file://name-baby \
"

SRC_URI_append_fab4 = " \
	file://slimproto-fab4.patch;patch=1 \
	file://config-fab4 \
	file://name-fab4 \
"

SRC_URI_append_jive = " \
	file://slimproto-jive.patch;patch=1 \
	file://config-jive \
	file://name-jive \
"

S = "${WORKDIR}/${PN}-${PV}-${PR}"

ARM_INSTRUCTION_SET = "arm"

CFLAGS_prepend = -I${STAGING_INCDIR}/opus -std=gnu99
CFLAGS_prepend_baby = '-DMODEL_NAME="Squeezebox Radio" -DCUSTOM_VERSION=-baby '
CFLAGS_prepend_fab4 = '-DMODEL_NAME="Squeezebox Touch" -DCUSTOM_VERSION=-fab4 '
CFLAGS_prepend_jive = '-DMODEL_NAME="Controller" -DCUSTOM_VERSION=-jive '
EXTRA_OEMAKE = '-f ../Makefile.squeezeos "OPTS=-DVISEXPORT -DRESAMPLE -DUSE_SSL -DOPUS -DFFMPEG -DTREMOR_ONLY"'

do_compile() {
        oe_runmake
	${CC} ${TARGET_CFLAGS} -o alsacap -l asound tools/alsacap.c
}

do_install() {
        install -m 0755 -d ${D}${bindir}
        install -m 0755 ${S}/${PN} ${D}${bindir}/${PN}
        install -m 0755 ${S}/alsacap ${D}${bindir}/alsacap
        install -m 0755 -d ${D}/etc/${PN}
        install -m 0755 ${S}/../updateconfig ${D}/etc/squeezelite
        install -m 0755 -d ${D}/etc/init.d
        install -m 0755 ${S}/../initd ${D}/etc/init.d/${PN}

        # Settings applet
        install -m 0755 -d ${D}${datadir}/jive/applets/SetupSqueezelite
        install -m 0644 ${WORKDIR}/SetupSqueezelite/SetupSqueezeliteApplet.lua ${D}${datadir}/jive/applets/SetupSqueezelite/SetupSqueezeliteApplet.lua
        install -m 0644 ${WORKDIR}/SetupSqueezelite/SetupSqueezeliteMeta.lua ${D}${datadir}/jive/applets/SetupSqueezelite/SetupSqueezeliteMeta.lua
        install -m 0644 ${WORKDIR}/SetupSqueezelite/loadPriority.lua ${D}${datadir}/jive/applets/SetupSqueezelite/loadPriority.lua
        install -m 0644 ${WORKDIR}/SetupSqueezelite/strings.txt ${D}${datadir}/jive/applets/SetupSqueezelite/strings.txt
}

do_install_append_baby() {
	install -m 0644 ${S}/../name-baby ${D}/etc/squeezelite/name
	install -m 0644 ${S}/../config-baby ${D}/etc/squeezelite/config
}

do_install_append_fab4() {
	install -m 0644 ${S}/../name-fab4 ${D}/etc/squeezelite/name
	install -m 0644 ${S}/../config-fab4 ${D}/etc/squeezelite/config
}

do_install_append_jive() {
	install -m 0644 ${S}/../name-jive ${D}/etc/squeezelite/name
	install -m 0644 ${S}/../config-jive ${D}/etc/squeezelite/config
}

FILES_${PN} = " \
    ${bindir}/${PN} \
    ${bindir}/alsacap \
    ${datadir}/jive/applets/SetupSqueezelite \
    /etc/init.d/squeezelite \
    /etc/squeezelite/name \
    /etc/squeezelite/config \
    /etc/squeezelite/updateconfig \
"
