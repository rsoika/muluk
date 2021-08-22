package org.imixs.muluk;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.imixs.muluk.xml.XMLCluster;
import org.imixs.muluk.xml.XMLConfig;
import org.imixs.muluk.xml.XMLMonitor;
import org.imixs.muluk.xml.XMLObject;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test class reads the "/document-example.xml" file via the
 * XMLItemCollectionAdapter and verifies the content. Than the test case writes
 * the data back into a new file "/document-example.xml" on the same file
 * system.
 * 
 * 
 * @author rsoika
 */
public class TestReadXMLConfig {

	/**
	 * Read /document-example.xml
	 */
	@Test
	public void testRead() {
		XMLConfig conf = null;

		try {
			conf = readCollectionFromInputStream(getClass().getResourceAsStream("/config.xml"));
		} catch (JAXBException e) {
			Assert.fail();
		} catch (IOException e) {
			Assert.fail();
		}

		Assert.assertNotNull(conf);

		XMLCluster cluster = conf.getCluster();
		Assert.assertNotNull(cluster);
		
		Assert.assertEquals("sepp",cluster.getName());

		Assert.assertTrue(cluster.getNode().length == 1);
		// test values
		
		XMLMonitor objects = conf.getMonitor();
		Assert.assertNotNull(objects);
		Assert.assertTrue(objects.getObject().length == 1);
		
		XMLObject object = objects.getObject()[0];
		Assert.assertEquals("web",object.getType());
		Assert.assertEquals("https://www.imixs.org",object.getTarget());
	}

	/**
	 * This method imports an xml entity data stream and returns a List of
	 * ItemCollection objects. The method can import any kind of entity data like
	 * model or configuration data an xml export of workitems.
	 * 
	 * @param inputStream xml input stream
	 * @throws JAXBException
	 * @throws IOException
	 * @return List of ItemCollection objects
	 */
	public static XMLConfig readCollectionFromInputStream(InputStream inputStream) throws JAXBException, IOException {
		byte[] byteInput = null;

		if (inputStream == null) {
			return null;
		}
		byteInput = getBytesFromStream(inputStream);
		return readCollection(byteInput);

	}

//    public static byte[] writeItemCollection(ItemCollection document) throws JAXBException, IOException {
//        if (document == null) {
//            return null;
//        }
//        XMLDataCollection ecol = XMLDataCollectionAdapter.getDataCollection(document);
//        StringWriter writer = new StringWriter();
//        JAXBContext context = JAXBContext.newInstance(XMLDataCollection.class);
//        Marshaller m = context.createMarshaller();
//        m.marshal(ecol, writer);
//        return writer.toString().getBytes();
//    }

	public static byte[] getBytesFromStream(InputStream is) throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		int nRead;
		byte[] data = new byte[0x4000];
		while ((nRead = is.read(data, 0, data.length)) != -1) {
			buffer.write(data, 0, nRead);
		}
		buffer.flush();
		is.close();
		return buffer.toByteArray();
	}

	public static XMLConfig readCollection(byte[] byteInput) throws JAXBException, IOException {

		if (byteInput == null || byteInput.length == 0) {
			return null;
		}

		XMLConfig ecol = null;

		JAXBContext context = JAXBContext.newInstance(XMLConfig.class);
		Unmarshaller m = context.createUnmarshaller();

		ByteArrayInputStream input = new ByteArrayInputStream(byteInput);
		Object jaxbObject = m.unmarshal(input);
		if (jaxbObject == null) {
			throw new RuntimeException("readCollection error - wrong xml file format - unable to read content!");
		}

		ecol = (XMLConfig) jaxbObject;

		return ecol;

	}

}
