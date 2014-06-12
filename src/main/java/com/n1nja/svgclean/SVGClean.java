package com.n1nja.svgclean;
import java.io.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.*;
import org.xml.sax.SAXException;
public class SVGClean {
	static public final String SVG_NS = "http://www.w3.org/2000/svg";
	static public final String XLINK_NS = "http://www.w3.org/1999/xlink";
	static public final void main(final String[] args) {
		if (args.length < 1) throw new IllegalArgumentException("Please specify the paths of the SVG documents to clean.");
		final DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
		dbfac.setValidating(true);
		dbfac.setIgnoringComments(true);
		dbfac.setNamespaceAware(true);
		dbfac.setIgnoringElementContentWhitespace(true);
		final DocumentBuilder db;
		try {
			db = dbfac.newDocumentBuilder();
		} catch (final ParserConfigurationException pce) {
			throw new RuntimeException(pce);
		}
		final TransformerFactory tfac = TransformerFactory.newInstance();
		final Transformer traf;
		try {
			traf = tfac.newTransformer();
		} catch (final TransformerConfigurationException e) {
			throw new RuntimeException(e);
		}
		for (final String arg : args) {
			final Document doc;
			try {
				doc = db.parse(arg);
			} catch (final SAXException | IOException | IllegalArgumentException e) {
				throw new RuntimeException("Invalid URI", e);
			}
			final Element svg = doc.getDocumentElement();
			for (final String attr : new String[] {
				"id",
				"style",
				"xml:space",
			}) {
				svg.removeAttribute(attr);
			}
			final NodeList paths = svg.getElementsByTagNameNS(SVG_NS, "path");
			for (int i = 0; i < paths.getLength(); i++) {
				final Element path = ((Element) paths.item(i));
				final String d = path.getAttribute("d");
				path.setAttribute("d", d.replaceAll("\\s+", " "));
			}
			doc.normalize();
			traf.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "-//W3C//DTD SVG 1.1//EN");
			traf.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd");
			traf.setOutputProperty(OutputKeys.INDENT, "yes");
			final DOMSource src = new DOMSource(doc);
			final StreamResult res;
			try {
				res = new StreamResult(new BufferedOutputStream(new FileOutputStream(new File(String.format("%1$s_clean.svg", arg.substring(0, arg.indexOf(".svg")))))));
			} catch (final FileNotFoundException e) {
				throw new RuntimeException(e);
			}
			try {
				traf.transform(src, res);
			} catch (final TransformerException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
