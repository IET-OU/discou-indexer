discou-indexer
==============

The DiscOU indexer

#### How to build an index
You need to write some Java code to plug your data sources into the DiscouIndexer. 
A simple example of how this can be done is the following:
```
DiscouIndexer indexer = new DiscouIndexer(new File(location), "http://spotlight.sztaki.hu:2222/rest");
indexer.open();
try {
  indexer.put(new DiscouInputResourceImpl(uri, title, description, content));
  // or
  Iterator<DiscouInputResource> resources = listResources() /// implement this your own way
  while(resources.hasNext()){
     indexer.put(resources.next());
  }
} catch (IOException e) {
	log.error("",e);
}finally{
	try {
		indexer.commit();
	} catch (IOException e) {
		log.error("",e);
	}finally{
		try {
			indexer.close();
		} catch (IOException e) {
		}				
	}
}
```

A convenient way of doing it is to setup a project used the (https://github.com/the-open-university/discou-indexer-archetype)[discou-indexer-archetype] maven archetype.
