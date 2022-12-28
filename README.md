# Blog Promoter

This service automatically promotes content published on relevant blogs, including [on the Spring blog](https://spring.io/blog).

## To Do 
- figure out how to remove the focus on spring and generify it so that it works for any feed input 
- figure how to turn the application into a native image 
- how do i create `N` `IntegrationFlow` instances based on `N` feed URIs without any _a priori_ knowledge? I know Spring Integration has a facility for this. 
- figure how to provide a strategy interface for everything unique to the strategy for a particular blog. 
  - this should include things like RSS -> twitter mappings 
  - providing templated tweet text given inputs like the feed's `SynEntry`
- the reason why we need the twitter text to be templatizable is because I want to isolate the special logic around identifying the spring team when tweeting as part of the Spring implementation of the strategy  

