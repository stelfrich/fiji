package fiji.spimdata;

//import static mpicbg.spim.data.newstuff.SpimDataXmlKeys.*;

import java.io.File;
import java.io.StringWriter;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.XmlHelpers;
import mpicbg.spim.data.XmlIoSpimData;
import mpicbg.spim.data.registration.XmlIoViewRegistrations;
import mpicbg.spim.data.sequence.TimePoint;
import mpicbg.spim.data.sequence.ViewDescription;
import mpicbg.spim.data.sequence.ViewSetup;
import mpicbg.spim.data.sequence.XmlIoImgLoader;
import mpicbg.spim.data.sequence.XmlIoMissingViews;
import mpicbg.spim.data.sequence.XmlIoSequenceDescription;
import mpicbg.spim.data.sequence.XmlIoTimePoints;
import mpicbg.spim.data.sequence.XmlIoViewSetups;

import org.w3c.dom.Document;

import fiji.spimdata.sequence.ViewSetupBeads;
import fiji.spimdata.sequence.XmlIoViewSetupsBeads;

public class SpimDataExample
{
	public static void main( final String[] args ) throws Exception
	{
		// load SpimData from xml file
		final String xmlFilename = "/Users/preibischs/workspace/fiji/src-plugins/SPIM_Registration/src/main/resources/example_fromdialog.xml";
		
		//final XmlIoSpimData< TimePoint, ViewSetupBeads > io = XmlIoSpimData.createDefault();
		final XmlIoSequenceDescription< TimePoint, ViewSetupBeads > seqDesc = 
				new XmlIoSequenceDescription< TimePoint, ViewSetupBeads >( new XmlIoTimePoints(), new XmlIoViewSetupsBeads(), new XmlIoMissingViews(), new XmlIoImgLoader() );
		final XmlIoSpimData< TimePoint, ViewSetupBeads > io = 
				new XmlIoSpimData< TimePoint, ViewSetupBeads >( seqDesc, new XmlIoViewRegistrations() );
		final SpimData< TimePoint, ViewSetupBeads > spimData = io.load( xmlFilename );	
		
		// save SpimData to xml file
		io.save( spimData, "example_new2.xml" );

		// write SpimData into a xml Document
		final Document doc = XmlHelpers.newXmlDocument();
		doc.appendChild( io.toXml( doc, spimData, new File(".") ) );

		// output Document to System.out
		final Transformer transformer = TransformerFactory.newInstance().newTransformer();
		transformer.setOutputProperty( OutputKeys.INDENT, "yes" );
		transformer.setOutputProperty( OutputKeys.ENCODING, "UTF-8" );
		transformer.setOutputProperty( "{http://xml.apache.org/xslt}indent-amount", "4" );
		final StringWriter w = new StringWriter();
		transformer.transform( new DOMSource( doc ), new StreamResult( w ) );
		System.out.println( w );
	}
}