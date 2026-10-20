DESCRIPTION = "SqueezePlay"
LICENSE = "Logitech Public Source License"

PV = "${DISTRO_VERSION}+svnr${SRCREV}"
PR = "r26"

DEPENDS += "libsdl libsdl-ttf libsdl-gfx libsdl-image"
DEPENDS += "lua lua-native luatolua++"
DEPENDS += "flac libmad tremor fdk-aac"

RDEPENDS += "liblua5.1-socket liblua5.1-json liblua5.1-zipfilter liblua5.1-loop liblua5.1-filesystem liblua5.1-profiler liblua5.1-tolua++ liblua5.1-md5 liblua5.1-expat"
RDEPENDS += "freefont"

SRC_URI = "${SQUEEZEPLAY_SCM};module=squeezeplay \
	file://logconf.lua"

SRC_URI_append_baby = " \
	file://0001-Initialize-effects-resampler-while-pcm-closed.patch;patch=1 \
	file://0002-Eliminate-bass-drop-out-by-disabling-XRUN-and-substi.patch;patch=1 \
	"
SRC_URI_append_fab4 = " \
	file://0004-support-for-auto-detect-of-alsa-sample-format.patch;patch=1 \
	file://0005-wait-for-1-sec-worth-of-decoded-samples-before-start.patch;patch=1 \
	file://0006-add-support-of-usb-output-devices-which-become-unava.patch;patch=1 \
	file://0007-add-ability-to-fallback-to-plughw-for-hw-device-if-r.patch;patch=1 \
	file://0008-protect-against-corrupt-sample-rate-if-7.8-server-us.patch;patch=1 \
	file://0009-experimental-cpu-randomisation.patch;patch=1 \
	file://0010-ignore-tuned-buffer-values-when-using-plug-layer.patch;patch=1 \
	file://0011-force-effects-device-sample-format.patch;patch=1 \
	file://EnhancedDigitalOutput \
	"

S = "${WORKDIR}/squeezeplay"

ARM_INSTRUCTION_SET = "arm"

inherit autotools

EXTRA_OECONF = "--disable-portaudio --enable-fsync-workaround"

EXTRA_OECONF_append_baby = " --enable-screen-rotation"

# Optional close source package
DEPENDS += "${@base_conditional('ENABLE_SPPRIVATE', 'yes', 'squeezeplay-private', '', d)}"
EXTRA_OECONF += "${@base_conditional('ENABLE_SPPRIVATE', 'yes', '--with-spprivate', '', d)}"

CFLAGS_prepend = '-DSQUEEZEPLAY_RELEASE=\\"${DISTRO_RELEASE}\\" -DSQUEEZEPLAY_REVISION=\\"${@squeezeos_squeezeplay_revision(d)}\\"'

EXTRA_OEMAKE = "all lua-lint"

python do_make_squeezeos_squeezeplay_revision() {
	import bb, os
	
	# 4 cases to consider: SqueezeOS and SqueezePlay could each have come from
	# either subversion or git. Use length of revision string and presence
	# of subversion (.svn) directory and .git_revision_count file to decide.
	
	osRevision = bb.data.getVar('SQUEEZEOS_REVISION', d, 1)
	if len(osRevision) > 8:	# assume a git revision hash
		s = os.popen("cd %s; git rev-list %s -- | wc -l" % (bb.data.getVar('OEROOT', d, 1), bb.data.getVar('METADATA_REVISION', d, 1))).read().strip()
		osrev = "%05d" % int(s)
	else:
		osrev = osRevision

	
	os.chdir(bb.data.getVar('S', d, 1))
	if os.path.exists('.svn'):
		if len(osRevision) > 8:	# assume a git revision hash
			# OS=git, SP=svn
			sprev = os.popen("svn info | sed -n 's/^Revision: //p' | head -1").read().strip()
		else:
			# OS=svn, S=svn
			# Assume (require) same repository
			sprev = ""
	elif os.path.exists('.git_revision_count'):
		sprev = open(os.path.join(bb.data.getVar('S', d, 1), '.git_revision_count'), 'r').readline().strip()
		if (len(osrev) < 9):
			osrev = "%d" % (int(osrev) + int(sprev) + 10000)
			sprev = ""
	else:
		# SP type unknown - just use OS revision
		sprev = ""
		
	staging = bb.data.getVar('STAGING_DIR_TARGET', d, 1)
	bb.mkdirhier(staging)
	revision = "%s%s" % (sprev, osrev)
	open(os.path.join(staging, 'squeezeos_squeezeplay_revision'), 'w').write("%s\n" % revision)
}

addtask make_squeezeos_squeezeplay_revision after do_unpack before do_configure

def squeezeos_squeezeplay_revision(d):
	import bb, os
	return open(os.path.join(bb.data.getVar('STAGING_DIR_TARGET', d, 1), 'squeezeos_squeezeplay_revision'), 'r').readline().strip()

do_stage() {
	install -d ${STAGING_INCDIR}/squeezeplay
	install -d ${STAGING_INCDIR}/squeezeplay/ui
	install -d ${STAGING_INCDIR}/squeezeplay/audio
	install -m 0644 src/log.h ${STAGING_INCDIR}/squeezeplay/log.h
	install -m 0644 src/config.h ${STAGING_INCDIR}/squeezeplay/config.h
	install -m 0644 src/common.h ${STAGING_INCDIR}/squeezeplay/common.h
	install -m 0644 src/types.h ${STAGING_INCDIR}/squeezeplay/types.h
	install -m 0644 src/ui/jive.h ${STAGING_INCDIR}/squeezeplay/ui/jive.h
	install -m 0644 src/audio/fifo.h ${STAGING_INCDIR}/squeezeplay/audio/fifo.h
	install -m 0644 src/audio/streambuf.h ${STAGING_INCDIR}/squeezeplay/audio/streambuf.h
}

do_install_append() {
	install -m 0644 ${WORKDIR}/logconf.lua ${D}${datadir}/jive/logconf.lua

}

do_install_append_fab4() {
        # Audio Settings applet
        install -m 0755 -d ${D}${datadir}/jive/applets/EnhancedDigitalOutput
        install -m 0644 ${WORKDIR}/EnhancedDigitalOutput/EnhancedDigitalOutputApplet.lua ${D}${datadir}/jive/applets/EnhancedDigitalOutput/EnhancedDigitalOutputApplet.lua
        install -m 0644 ${WORKDIR}/EnhancedDigitalOutput/EnhancedDigitalOutputMeta.lua ${D}${datadir}/jive/applets/EnhancedDigitalOutput/EnhancedDigitalOutputMeta.lua
        install -m 0644 ${WORKDIR}/EnhancedDigitalOutput/loadPriority.lua ${D}${datadir}/jive/applets/EnhancedDigitalOutput/loadPriority.lua
        install -m 0644 ${WORKDIR}/EnhancedDigitalOutput/strings.txt ${D}${datadir}/jive/applets/EnhancedDigitalOutput/strings.txt
}

PACKAGES = "${PN}-dbg ${PN}-qvgaskin ${PN}-jiveskin ${PN}-fab4skin ${PN}-babyskin ${PN}"

FILES_${PN}-qvgaskin += "\
	${datadir}/jive/applets/QVGAbaseSkin \
"

FILES_${PN}-jiveskin += "\
	${datadir}/jive/applets/QVGAportraitSkin \
"

FILES_${PN}-babyskin += "\
	${datadir}/jive/applets/QVGAlandscapeSkin \
"

FILES_${PN}-fab4skin += "\
	${datadir}/jive/applets/WQVGAlargeSkin \
	${datadir}/jive/applets/WQVGAsmallSkin \
"

FILES_${PN} += "${datadir}"
