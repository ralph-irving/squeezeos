DESCRIPTION = "tzcode, timezone zoneinfo utils -- zic, zdump, tzselect"

PR = "r0"

SRC_URI = " \
	https://data.iana.org/time-zones/releases/tzcode${PV}.tar.gz \
	https://data.iana.org/time-zones/releases/tzdata${PV}.tar.gz \
"

S = "${WORKDIR}"

inherit native

CFLAGS_prepend = " -std=c99 "

do_stage () {
        install -d ${STAGING_BINDIR_NATIVE}
        install -m 755 zic ${STAGING_BINDIR_NATIVE}/
        install -m 755 zdump ${STAGING_BINDIR_NATIVE}/
        install -m 755 tzselect ${STAGING_BINDIR_NATIVE}/
}

do_install () {
        :
}
