package com.n1nja.svgclean;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.commons.cli.*;
import org.w3c.dom.*;
import org.xml.sax.SAXException;
public class SVGCleaner implements Runnable {
	static public final String SVG_NS = "http://www.w3.org/2000/svg";
	static public final String XLINK_NS = "http://www.w3.org/1999/xlink";
	static private final DocumentBuilderFactory SVG_DB_FACTORY;
	static private final TransformerFactory SVG_T_FACTORY;
	static private final Options CLI_OPTIONS;
	static {
		SVG_DB_FACTORY = DocumentBuilderFactory.newInstance();
		SVG_DB_FACTORY.setValidating(true);
		SVG_DB_FACTORY.setIgnoringComments(true);
		SVG_DB_FACTORY.setNamespaceAware(true);
		SVG_DB_FACTORY.setIgnoringElementContentWhitespace(true);
		SVG_T_FACTORY = TransformerFactory.newInstance();
		CLI_OPTIONS = new Options();
		CLI_OPTIONS.addOption("s", "stdout", false, "print the results to stdout");
		CLI_OPTIONS.addOption(null, "no-clean", false, "don't clean SVG, just format");
	}
	static public void main(final String[] args) throws ParseException {
		final CommandLineParser cli = new PosixParser();
		final CommandLine cl = cli.parse(CLI_OPTIONS, args);
		final List<SVGCleaner> cleaners = new ArrayList<>();
		for (final String arg : cl.getArgs()) {
			cleaners.add(new SVGCleaner(arg));
		}
		if (!cl.hasOption("no-clean")) {
			final ExecutorService pool = Executors.newCachedThreadPool();
			for (final SVGCleaner cleaner : cleaners) {
				pool.execute(cleaner);
			}
			pool.shutdown();
			try {
				pool.awaitTermination(1L, TimeUnit.MINUTES);
			} catch (final InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		if (cl.hasOption("stdout")) {
			for (final SVGCleaner cleaner : cleaners) {
				cleaner.transform(new StreamResult(System.out));
				System.out.println();
			}
		} else {
			for (final SVGCleaner cleaner : cleaners) {
				try {
					cleaner.transform(new StreamResult(new BufferedOutputStream(new FileOutputStream(cleaner.getURI().replaceAll(Pattern.quote(".svg") + "$", "-clean.svg")))));
				} catch (final FileNotFoundException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}
	private final DocumentBuilder db;
	private final String URI;
	private final Document doc;
	{
		try {
			this.db = SVG_DB_FACTORY.newDocumentBuilder();
		} catch (final ParserConfigurationException e) {
			throw new Error(e);
		}
	}
	public SVGCleaner(final String URI) {
		if (URI == null) throw new IllegalArgumentException();
		this.URI = URI;
		try {
			this.doc = this.db.parse(this.URI);
		} catch (final SAXException | IOException | IllegalArgumentException e) {
			throw new RuntimeException("Invalid URI", e);
		}
	}
	public void clean() {
		final Element svg = this.doc.getDocumentElement();
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
		this.doc.normalize();
	}
	public Document getDocument() {
		return this.doc;
	}
	public String getString() {
		final ByteArrayOutputStream str = new ByteArrayOutputStream();
		final StreamResult res = new StreamResult(str);
		this.transform(res);
		return String.valueOf(str.toByteArray());
	}
	public String getURI() {
		return this.URI;
	}
	@Override
	public void run() {
		this.clean();
	}
	public void transform(final Result res) {
		final Transformer traf;
		try {
			traf = SVG_T_FACTORY.newTransformer();
		} catch (final TransformerConfigurationException e) {
			throw new Error(e);
		}
		traf.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "-//W3C//DTD SVG 1.1//EN");
		traf.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd");
		traf.setOutputProperty(OutputKeys.INDENT, "yes");
		final DOMSource src = new DOMSource(this.doc);
		try {
			traf.transform(src, res);
		} catch (final TransformerException e) {
			throw new RuntimeException(e);
		}
	}
}
