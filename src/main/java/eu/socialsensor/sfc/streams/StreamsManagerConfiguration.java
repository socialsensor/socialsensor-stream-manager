package eu.socialsensor.sfc.streams;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import eu.socialsensor.framework.client.mongo.MongoHandler;
import eu.socialsensor.framework.common.domain.Item;
import eu.socialsensor.framework.common.factories.ItemFactory;
import eu.socialsensor.framework.streams.StreamConfiguration;
import eu.socialsensor.sfc.streams.Configuration;

public class StreamsManagerConfiguration extends Configuration {
	

	/**
	 * 
	 */
	private static final long serialVersionUID = 5269944272731808440L;

	@Expose
	@SerializedName(value = "streams")
	private Map<String, StreamConfiguration> streamConfigMap = null;
	
	@Expose
	@SerializedName(value = "storages")
	private Map<String, StorageConfiguration> storageConfigMap = null;
	
	@Expose
	@SerializedName(value = "request_period")
	private String requestPeriod = null;
	
	public StreamsManagerConfiguration() {
		streamConfigMap = new HashMap<String, StreamConfiguration>();
		storageConfigMap = new HashMap<String, StorageConfiguration>();
	}
	
	public void setStreamConfig(String streamId, StreamConfiguration config){
		streamConfigMap.put(streamId,config);
	}
	
	
	public StreamConfiguration getStreamConfig(String streamId){
		return streamConfigMap.get(streamId);
	}
	
	public void setStorageConfig(String storageId, StorageConfiguration config){
		storageConfigMap.put(storageId,config);
	}
	
	
	public StorageConfiguration getStorageConfig(String storageId){
		return storageConfigMap.get(storageId);
	}
	
	public void setParameter(String name, String value){
		super.setParameter(name,value);
	}
	
	
	public String getParameter(String name) {
		return super.getParameter(name);
	}
	
	public Set<String> getStreamIds() {
		return streamConfigMap.keySet();
	}
	
	public Set<String> getStorageIds() {
		return storageConfigMap.keySet();
	}
	
	public String getRequestPeriod(){
		return requestPeriod;
	}
	
	public static StreamsManagerConfiguration readFromMongo(String host, String dbName, String collectionName) 
			throws UnknownHostException {
		MongoHandler mongo = new MongoHandler(host, dbName, collectionName, null);
		
		String json = mongo.findOne();
		Gson gson = new GsonBuilder()
        	.excludeFieldsWithoutExposeAnnotation()
        	.create();
		StreamsManagerConfiguration config = gson.fromJson(json, StreamsManagerConfiguration.class);
        return config;
	}
	
	public void readItemsFromMongo(String host, String dbName, String collection, List<Item> mongoItems) throws UnknownHostException{
		MongoHandler mongo = new MongoHandler(host, dbName, collection, null);
		List<String> jsonItems = mongo.findMany(-1);
	
		for(String json : jsonItems){
			
			Item item = ItemFactory.create(json);
			
			mongoItems.add(item);
			
		}
		
	}
	
	public void saveToMongo(String host, String dbName, String collectionName) 
			throws UnknownHostException {
		MongoHandler mongo = new MongoHandler(host, dbName, collectionName, null);
		
		mongo.insert(this);
		
	}
	
	public static StreamsManagerConfiguration readFromFile(File file) 
			    throws ParserConfigurationException, SAXException, IOException {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser parser = factory.newSAXParser();
		ParseHandler handler = new ParseHandler();
		parser.parse(file, handler);
		return handler.getConfig();
	}
	
	
	private static class ParseHandler extends DefaultHandler {

		private enum ParseState{
			IDLE,
			IN_CONFIG_PARAM,
			IN_CONFIG_STREAM,
			IN_CONFIG_STREAM_PARAM,
			IN_CONFIG_STORAGE,
			IN_CONFIG_STORAGE_PARAM,
			IN_CONFIG_REQUEST,
			IN_CONFIG_REQUEST_PARAM,
		}
		
		private StreamsManagerConfiguration config = new StreamsManagerConfiguration();
	    private ParseState state = ParseState.IDLE;
	    private StringBuilder value = null;
	    private String name = null;
	    private StreamConfiguration sconfig = null;
	    private StorageConfiguration storage_config = null; 
	    private String streamId = null, storageId = null;
	    private String requestPeriod = null;
		
		public StreamsManagerConfiguration getConfig() {
			return config;
		}

		@Override
		public void startElement(String uri, String localName, String name,
				Attributes attributes) throws SAXException {
			
			//System.out.println("IN: "+name);
			
			if (name.equalsIgnoreCase("Parameter")) {
				this.name = attributes.getValue("name");
				if (this.name == null) return;
				value = new StringBuilder();
				if (state == ParseState.IDLE) {
					state = ParseState.IN_CONFIG_PARAM;
				}else if(state == ParseState.IN_CONFIG_STREAM) {
					state = ParseState.IN_CONFIG_STREAM_PARAM;
				}
				else if(state == ParseState.IN_CONFIG_STORAGE) {
					state = ParseState.IN_CONFIG_STORAGE_PARAM;
				}
				else if(state == ParseState.IN_CONFIG_REQUEST){
					state = ParseState.IN_CONFIG_REQUEST_PARAM;
				}
			}
			
			else if (name.equalsIgnoreCase("Stream")){
				streamId = attributes.getValue("id");
				value = new StringBuilder();
				if (streamId == null) return;
				sconfig = new StreamConfiguration();
				state = ParseState.IN_CONFIG_STREAM;
			}
			
			else if (name.equalsIgnoreCase("Storage")){
				storageId = attributes.getValue("id");
				value = new StringBuilder();
				if (storageId == null) return;
				storage_config = new StorageConfiguration();
				state = ParseState.IN_CONFIG_STORAGE;
			}
			else if (name.equalsIgnoreCase("RequestPeriod")){
				value = new StringBuilder();
				state = ParseState.IN_CONFIG_REQUEST;
			}
		}
		
		@Override
		public void characters(char[] ch, int start, int length)
				throws SAXException {
			if (value != null){
				value.append(ch, start, length);
			}
		}

		@Override
		public void endElement(String uri, String localName, String name)
				throws SAXException {

			if (name.equalsIgnoreCase("Parameter") && state == ParseState.IN_CONFIG_PARAM) {
				config.setParameter(this.name, value.toString().trim());
				state = ParseState.IDLE;
			}
			else if (name.equalsIgnoreCase("Parameter") && state == ParseState.IN_CONFIG_STREAM_PARAM){
				sconfig.setParameter(this.name, value.toString().trim());
				state = ParseState.IN_CONFIG_STREAM;
			}
			else if (name.equalsIgnoreCase("Parameter") && state == ParseState.IN_CONFIG_STORAGE_PARAM){
				storage_config.setParameter(this.name, value.toString().trim());
				state = ParseState.IN_CONFIG_STORAGE;
			}
			else if (name.equalsIgnoreCase("Stream")){
				config.setStreamConfig(streamId, sconfig);
				state = ParseState.IDLE;
			}
			else if (name.equalsIgnoreCase("Storage")){
				config.setStorageConfig(storageId, storage_config);
				state = ParseState.IDLE;
			}
			else if(name.equalsIgnoreCase("Parameter") && state == ParseState.IN_CONFIG_REQUEST_PARAM){
				config.setParameter(this.name, value.toString().trim());
				state = ParseState.IN_CONFIG_REQUEST;
			}
				
		}

		
		
	}
}
