DESCRIPTION = "Timezone data for SqueezeOS"
SECTION  = "base"
PRIORITY = "optional"

# This recipe is a customized version of 'tzdata', which builds the timezone
# database '/usr/share/zoneinfo'. It replaces the timezone database provided
# by the CSL tool chain.

# The recipe 'tzdata' cannot be used 'as is' for this purpose, because it does
# not compile suitable timezone files for this older 32 bit OS.

# It is intended to be automatically invoked by recipe
#  'external-csl-toolchain_2010q1-202-modified.bb', which is a modified
# version of the CSL tool chain that suppresses timezone database generation.

PR = "r0"

# Ideally, the version of 'tzcode-native' should match the version of this
# recipe, but Bitbake does not provide for versioned dependencies.
# The 'do_compile' function will check that the version of the timezone
# compiler 'zic' does, indeed, match.
DEPENDS  = "tzcode-native"

# This package will replace any packages provided by the 'tzdata' recipe.
RCONFLICTS = "tzdata tzdata-africa tzdata-americas tzdata-antarctica \
              tzdata-arctic tzdata-asia tzdata-atlantic \
              tzdata-australia tzdata-europe tzdata-misc \
              tzdata-pacific tzdata-posix tzdata-right"

# And any earlier packages. The 'tzdata' recipe identifies this earlier package.
RCONFLICTS += "timezones timezone-africa timezone-america timezone-antarctica \
               timezone-arctic timezone-asia timezone-atlantic \
               timezone-australia timezone-europe timezone-indian \
               timezone-iso3166.tab timezone-pacific timezone-zone.tab"

SRC_URI = "https://data.iana.org/time-zones/releases/tzdata${PV}.tar.gz \
           file://required_tzones.txt"


S = "${WORKDIR}"

# Available time zone data files are:
#   africa antarctica asia australasia europe northamerica
#   southamerica factory etcetera backward

# Time zone data files that we will build:
TZONES = "africa antarctica asia australasia europe northamerica \
          southamerica factory etcetera backward"

# Where we will place the compiled files, i.e. ${S}/usr/share/zoneinfo.
ZONEINFO_TGT = ${S}${datadir}/zoneinfo

do_compile () {

        #
        # Create the timezone (tzif) files.
        #

        # Check zic version - looks like 'zic (tzcode) YYYYx'
        ZICVER=$(${STAGING_BINDIR_NATIVE}/zic --version)
        case "$ZICVER" in
          *${PV}*) ;;
          *) oefatal "Package tzcode provides an unexpected zic version: $ZICVER. Version ${PV} expected.";;
        esac

        for zone in ${TZONES}; do
            # Need the '-b fat' option to generate tzif files compatible with
            # older 32 bit systems.
            ${STAGING_BINDIR_NATIVE}/zic -d ${ZONEINFO_TGT} -b fat \
              -L /dev/null ${S}/${zone};
            # If 'posix' & 'right' zoneinfo is needed, generate as follows:
            # ${STAGING_BINDIR_NATIVE}/zic -d ${ZONEINFO_TGT}/posix -b fat \
            #   -L /dev/null ${S}/${zone};
            # ${STAGING_BINDIR_NATIVE}/zic -d ${ZONEINFO_TGT}/right -b fat \
            #   -L ${S}/leapseconds ${S}/${zone};
        done

        #
        # Check that all required timezone files are present.
        #

        # Sanity check - grep errors will be lost in next pipeline
        [ -f ${WORKDIR}/required_tzones.txt -a -r ${WORKDIR}/required_tzones.txt ] || \
          oefatal "File 'required_tzones.txt' cannot be read."
        # strip blank lines/comment lines first
        grep -v '^[[:blank:]]*\(#\|$\)' ${WORKDIR}/required_tzones.txt | \
        while read name comment; do \
            [ -f ${ZONEINFO_TGT}/$name ] || \
              oefatal "Required timezone '$name' not present. Comment: $comment"
        done

        # If, for some reason, we have not generated a required
        # 'old-name -> new-name' timezone link, we could have made it and/or
        # checked it, as follows:
        #   old_name="Asia/Rangoon"
        #   new_name="Asia/Yangon"
        #   [ -f ${ZONEINFO_TGT}/$old_name ]  || \
        #     ln ${ZONEINFO_TGT}/$new_name ${ZONEINFO_TGT}/$old_name ;

        # Generate a posixrules file (substitute for zic -p option)
        # But not needed by SqueezeOS
        #ln ${ZONEINFO_TGT}/America/New_York ${ZONEINFO_TGT}/posixrules
}

do_install () {
        # make target directories available
        install -d -m 0755 ${D}${datadir}/zoneinfo
        install -d -m 0755 ${D}${sysconfdir}

        # copy over generated zoneinfo directory - preserving permissions
        cp -pPR ${ZONEINFO_TGT} ${D}${datadir}

        # supplementary files
        # But not needed by SqueezeOS
        #install -m 0644 ${S}/iso3166.tab  ${D}${datadir}/zoneinfo
        #install -m 0644 ${S}/zone.tab     ${D}${datadir}/zoneinfo
        #install -m 0644 ${S}/zone1970.tab ${D}${datadir}/zoneinfo

        # create /etc/localtime link
        ln -s ../usr/share/zoneinfo/Factory ${D}/${sysconfdir}/localtime
}


FILES_${PN} = "${datadir}/zoneinfo ${sysconfdir}"
