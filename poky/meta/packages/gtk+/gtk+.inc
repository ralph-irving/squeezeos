DESCRIPTION = "GTK+ is a multi-platform toolkit for creating graphical user interfaces. Offering a complete \
set of widgets, GTK+ is suitable for projects ranging from small one-off projects to complete application suites."
HOMEPAGE = "http://www.gtk.org"
SECTION = "libs"
LICENSE = "LGPL"
PRIORITY = "optional"
DEPENDS = "glib-2.0 pango atk jpeg libpng libxext libxcursor gtk-doc-native libxrandr \
           libgcrypt libxdamage libxrender libxcomposite cairo"

inherit autotools pkgconfig

FILES_${PN} = "${bindir}/gdk-pixbuf-query-loaders \
	${bindir}/gtk-update-icon-cache \
	${bindir}/gtk-query-immodules-2.0 \
	${libdir}/lib*${SOLIBS} \
	${datadir}/themes ${sysconfdir} \
	${libdir}/gtk-2.0/${LIBV}/engines/libpixmap.so"

FILES_${PN}-dev += " \
        ${datadir}/gtk-2.0/include \
	${libdir}/gtk-2.0/include \
	${libdir}/gtk-2.0/${LIBV}/loaders/*.la \
	${libdir}/gtk-2.0/${LIBV}/immodules/*.la \
	${libdir}/gtk-2.0/${LIBV}/engines/*.la \
	${bindir}/gdk-pixbuf-csource \
	${bindir}/gtk-builder-convert"

FILES_${PN}-dbg += " \
        ${libdir}/gtk-2.0/${LIBV}/loaders/.debug/* \
	${libdir}/gtk-2.0/${LIBV}/immodules/.debug/* \
	${libdir}/gtk-2.0/${LIBV}/engines/.debug/* \
	${libdir}/gtk-2.0/${LIBV}/printbackends/.debug/*"

BASE_RRECOMMENDS = "ttf-dejavu-sans"
BASE_RRECOMMENDS_angstrom = "ttf-dejavu-sans gdk-pixbuf-loader-png gdk-pixbuf-loader-jpeg gdk-pixbuf-loader-gif gdk-pixbuf-loader-xpm"
BASE_RRECOMMENDS_poky = "gdk-pixbuf-loader-png gdk-pixbuf-loader-jpeg gdk-pixbuf-loader-gif gdk-pixbuf-loader-xpm shared-mime-info"
BASE_RRECOMMENDS_moblin = "gdk-pixbuf-loader-png gdk-pixbuf-loader-jpeg gdk-pixbuf-loader-gif gdk-pixbuf-loader-xpm shared-mime-info"

GLIBC_RRECOMMENDS= "${BASE_RRECOMMENDS} glibc-gconv-iso8859-1"

RRECOMMENDS_${PN} = "${BASE_RRECOMMENDS}"
RRECOMMENDS_${PN}_linux = "${GLIBC_RRECOMMENDS}"
RRECOMMENDS_${PN}_linux-gnueabi = "${GLIBC_RRECOMMENDS}"

do_stage () {
	oe_libinstall -so -C gtk libgtk-x11-2.0 ${STAGING_LIBDIR}
	oe_libinstall -so -C gdk libgdk-x11-2.0 ${STAGING_LIBDIR}
	oe_libinstall -so -C contrib/gdk-pixbuf-xlib libgdk_pixbuf_xlib-2.0 ${STAGING_LIBDIR}
	oe_libinstall -so -C gdk-pixbuf libgdk_pixbuf-2.0 ${STAGING_LIBDIR}

	autotools_stage_includes

	mkdir -p ${STAGING_LIBDIR}/gtk-2.0/include
	install -m 0644 gdk/gdkconfig.h ${STAGING_LIBDIR}/gtk-2.0/include/gdkconfig.h

	install -m 0644 m4macros/gtk-2.0.m4 ${STAGING_DATADIR}/aclocal/

	install -m 0644 gtk/gtkfilechooserprivate.h ${STAGING_INCDIR}/gtk-2.0/gtk/
	install -m 0644 gtk/gtkfilechooserutils.h   ${STAGING_INCDIR}/gtk-2.0/gtk/
	install -m 0644 gtk/gtkfilesystemmodel.h    ${STAGING_INCDIR}/gtk-2.0/gtk/
}

do_install_append () {
	install -d ${D}${sysconfdir}/gtk-2.0
}

postinst_prologue() {
if [ "x$D" != "x" ]; then
  exit 1
fi

}

postinst_pixbufloader() {
if [ "x$D" != "x" ]; then
  exit 1
fi

gdk-pixbuf-query-loaders > ${sysconfdir}/gtk-2.0/gdk-pixbuf.loaders

test -x ${bindir}/gtk-update-icon-cache && gtk-update-icon-cache  -q ${datadir}/icons/hicolor
}
