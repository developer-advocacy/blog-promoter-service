# README   

## Motivation 

To automatically promote content being published [to the Spring blog](https://spring.io/blog). 

## Design Document

- Periodically screen scrape all the names from [the Spring teams page](https://spring.io/teams) and cache in a database (just in case the markup should change and i need time to fix it while blogs are being published)
- Write spring integration flow to monitor the RSS feed and then store the news articles in a DB along with a timestamp
    - do an upset here, so that if we ingest the same records over and over, given the same author and URL, then we get the same blog. Note the time of ingest.
    - note that it hasn't been promoted, yet
- then well have a periodic process that comes along and reads all the as-yet unprompted blogs and tweets out the ones that are at least half an hour old.
    - The reason for this is because it's common to have updates to blogs that change the title or fix spelling errors immediately after posting (I do this a lot)
- then if theyre not yet promoted and if it's been an hour or more since the last tweet was posted (from this process, at least), then send it to the twitter service c/o `@springcentral`

## To Do 
- x build parser for the team page
- x build spring integration feed processor
- x persistence for teammates
- x persistence for blog posts 
- x build jdbc metadataStore so that the spring integration feed adapter doesn't redeliver the same blog posts over and over 
- build out the periodic loop that pulls down the latest posts and tweets them
- deploy to Kubernetes

