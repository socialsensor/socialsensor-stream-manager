package eu.socialsensor.sfc.input;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import eu.socialsensor.framework.Configuration;
import eu.socialsensor.framework.client.dao.KeywordDAO;
import eu.socialsensor.framework.client.dao.RssSourceDAO;
import eu.socialsensor.framework.client.dao.SourceDAO;
import eu.socialsensor.framework.client.dao.impl.KeywordDAOImpl;
import eu.socialsensor.framework.client.dao.impl.RssSourceDAOImpl;
import eu.socialsensor.framework.client.dao.impl.SourceDAOImpl;
import eu.socialsensor.framework.common.domain.Feed;
import eu.socialsensor.framework.common.domain.Feed.FeedType;
import eu.socialsensor.framework.common.domain.feeds.KeywordsFeed;
import eu.socialsensor.framework.common.domain.feeds.ListFeed;
import eu.socialsensor.framework.common.domain.feeds.LocationFeed;
import eu.socialsensor.framework.common.domain.feeds.SourceFeed;
import eu.socialsensor.framework.common.domain.feeds.URLFeed;
import eu.socialsensor.framework.common.domain.Keyword;
import eu.socialsensor.framework.common.domain.Location;
import eu.socialsensor.framework.common.domain.NewsFeedSource;
import eu.socialsensor.framework.common.domain.SocialNetworkSource;
import eu.socialsensor.framework.common.domain.Source;


/**
 * @brief The class responsible for the creation of input feeds from
 * mongo db storage
 * @author ailiakop
 * @email  ailiakop@iti.gr
 */
public class MongoInputReader implements InputReader {
	
	protected static final String SINCE = "since";
	
	protected static final String HOST = "host";
	protected static final String DB = "database";
	protected static final String SOURCES_COLLECTION = "sources.collection";
	protected static final String LISTS_COLLECTION = "lists.collection";
	protected static final String RSS_SOURCES_COLLECTION = "rss_sources.collection";
	protected static final String EXPERTS_COLLECTION = "experts.collection";
	protected static final String KEYWORDS_COLLECTION = "keywords.collection";
	
	private Configuration storage_config;
	
	private Set<String> streams = new HashSet<String>();
	
	private String host = null;
	private String db = null;
	private String newsHoundsCollection = null;
	private String listsCollection = null;
	private String expertsCollection = null;
	private String keywordsCollection;
	
	private String streamType = null;
	
	private Date sinceDate = null;
	
	private Map<String, List<Feed>> feedsPerStream = new HashMap<String, List<Feed>>();

	private String rssSourcesCollection;
	
	public MongoInputReader(Configuration config) {
		
		this.storage_config = config;
		
		config.getParameter("since");
		
		streams.add("Twitter");
		streams.add("Facebook");
		streams.add("RSS");
		streams.add("Tumblr");
		streams.add("Instagram");
		streams.add("GooglePlus");
		streams.add("Youtube");
		streams.add("Flickr");
		
		this.host = storage_config.getParameter(MongoInputReader.HOST);
		this.db = storage_config.getParameter(MongoInputReader.DB);
		this.newsHoundsCollection = storage_config.getParameter(MongoInputReader.SOURCES_COLLECTION, "Sources");
		this.listsCollection = storage_config.getParameter(MongoInputReader.LISTS_COLLECTION, "Lists");
		this.rssSourcesCollection = storage_config.getParameter(MongoInputReader.RSS_SOURCES_COLLECTION, "RssSources");
		this.expertsCollection = storage_config.getParameter(MongoInputReader.EXPERTS_COLLECTION,"Experts");
		this.keywordsCollection = storage_config.getParameter(MongoInputReader.KEYWORDS_COLLECTION, "Keywords");
		
	}
	
	@Override
	public Map<String, List<Feed>> createFeedsPerStream() {
	
		for(String stream : streams) {
		
			List<Feed> feeds = new ArrayList<Feed>();
			
			if(stream.equals("Twitter")) {
				this.streamType = SocialNetworkSource.Twitter.name();
			}
			else if(stream.equals("Facebook")) {
				this.streamType = SocialNetworkSource.Facebook.name();
			}
			else if(stream.equals("Flickr")) {
				this.streamType = SocialNetworkSource.Flickr.name();
			}
			else if(stream.equals("GooglePlus")) {
				this.streamType = SocialNetworkSource.GooglePlus.name();
			}
			else if(stream.equals("Instagram")) {
				this.streamType = SocialNetworkSource.Instagram.name();
			}
			else if(stream.equals("Tumblr")) {
				this.streamType = SocialNetworkSource.Tumblr.name();
			}
			else if(stream.equals("Youtube")) {
				this.streamType = SocialNetworkSource.Youtube.name();
			}
			else if(stream.equals("RSS")) {
				this.streamType = NewsFeedSource.RSS.name();
			}
	
			Map<FeedType, Object> inputData = getData();

			for(FeedType feedType : inputData.keySet()) {

				switch(feedType) {
					case SOURCE :
						@SuppressWarnings("unchecked")
						List<Source> sources = (List<Source>) inputData.get(feedType);
						for(Source source : sources) {
							String feedID = source.getNetwork() + "#" + source.getName(); //UUID.randomUUID().toString();
							SourceFeed sourceFeed = new SourceFeed(source, sinceDate, feedID);
							sourceFeed.setLabel(source.getList());				
							feeds.add(sourceFeed);
						}
						break;
					
					case URL :
						@SuppressWarnings("unchecked")
						List<String> rssSources = (List<String>) inputData.get(feedType);
						for(String url : rssSources) {
							String feedID = url;//UUID.randomUUID().toString();
							URLFeed sourceFeed = new URLFeed(url, sinceDate, feedID);
							feeds.add(sourceFeed);
						}
						break;
				
					case KEYWORDS : 
						@SuppressWarnings("unchecked")
						List<Keyword> keywords = (List<Keyword>) inputData.get(feedType);
						for(Keyword keyword : keywords) {
							String feedID = UUID.randomUUID().toString();
							KeywordsFeed keywordsFeed = new KeywordsFeed(keyword, sinceDate, feedID);
							keywordsFeed.setLabel(keyword.getLabel());
							feeds.add(keywordsFeed);
						}
						break;
				
					case LOCATION :
						@SuppressWarnings("unchecked")
						List<Location> locations = (List<Location>) inputData.get(feedType);
						for(Location location : locations) {
							String feedID = UUID.randomUUID().toString();
							LocationFeed locationFeed = new LocationFeed(location, sinceDate, feedID);
							feeds.add(locationFeed);
						}
						break;
					
					case LIST :
						@SuppressWarnings("unchecked")
						List<Source> lists = (List<Source>) inputData.get(feedType);
						for(Source listSource : lists) {
							String feedID = UUID.randomUUID().toString();
							ListFeed listFeed = new ListFeed(listSource.getName(), sinceDate, feedID);
							listFeed.setLabel(listSource.getList());
							feeds.add(listFeed);
						}
						
						
					default:
						break;
				}
			}
			feedsPerStream.put(stream, feeds);
		}
		
		return feedsPerStream;
	}
	
	@Override
	public List<Feed> createFeeds(){
		return null;
	}
	
	@Override
	public Map<FeedType, Object> getData() {
		
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
		String since = storage_config.getParameter(SINCE);
		if(since != null) {
			try {
				sinceDate = (Date) formatter.parse(since);
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		
		Map<FeedType, Object> inputDataPerType = new HashMap<FeedType, Object>();
		
		if(host == null || db == null || newsHoundsCollection == null || expertsCollection == null){
			System.out.println("News hounds collection needs to be configured correctly");
			return null;
		}
		
		//sources
		List<Source> sources = new ArrayList<Source>();
		List<Source> lists = new ArrayList<Source>();
		List<String> rssSources = new ArrayList<String>();
		
		SourceDAO sourceDao = new SourceDAOImpl(host, db, newsHoundsCollection);
		SourceDAO listsDao = new SourceDAOImpl(host, db, listsCollection);
		RssSourceDAO rssSourceDao = new RssSourceDAOImpl(host, db, rssSourcesCollection);
		KeywordDAO keywordDao = new KeywordDAOImpl(host,db,keywordsCollection);
		
		if(streamType.equals("RSS")) {
			rssSources.addAll(rssSourceDao.getRssSources());
		}
		else {
			List<Source> streamSources = sourceDao.findTopSources(75000, SocialNetworkSource.valueOf(streamType));
			sources.addAll(streamSources);
		}
		
		// extract keywords
		List<Keyword> keywords;
		if(streamType.equals("RSS")) {
			keywords = new ArrayList<Keyword>();
		}
		else {
			keywords = keywordDao.findKeywords(SocialNetworkSource.valueOf(streamType));
		}
		
		// extract lists
		if(!streamType.equals("RSS")) {
			List<Source> listSources = listsDao.findTopSources(100, SocialNetworkSource.valueOf(streamType));
			lists.addAll(listSources);
		}

		if(!keywords.isEmpty()) {
			inputDataPerType.put(FeedType.KEYWORDS, keywords);
		}
		
		if(!sources.isEmpty()) {
			inputDataPerType.put(FeedType.SOURCE, sources);
		}
		
		if(!lists.isEmpty()) {
			inputDataPerType.put(FeedType.LIST, lists);
		}

		if(!rssSources.isEmpty()) {
			inputDataPerType.put(FeedType.URL, rssSources);
		}
		
		return inputDataPerType;
	}
	
}