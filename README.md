<!-- PROJECT SHIELDS -->
<!--
*** I'm using markdown "reference style" links for readability.
*** Reference links are enclosed in brackets [ ] instead of parentheses ( ).
*** See the bottom of this document for the declaration of the reference variables
*** for contributors-url, forks-url, etc. This is an optional, concise syntax you may use.
*** https://www.markdownguide.org/basic-syntax/#reference-style-links
-->
[![Download][download-shield]][download-url]
[![Jitpack][jitpack-shield]][jitpack-url]
[![Contributors][contributors-shield]][contributors-url]
[![Forks][forks-shield]][forks-url]
[![Stargazers][stars-shield]][stars-url]
[![Issues][issues-shield]][issues-url]
[![MIT License][license-shield]][license-url]
[![LinkedIn][linkedin-shield]][linkedin-url]



<!-- PROJECT LOGO -->
<br />
<div align="center">

<h3 align="center">Aseefian Proxy Pool [APP]</h3>

  <p align="center">
    A pure-java library built for managing large number of proxies!
    <br />
    <a href="https://github.com/Aseeef/AseefianProxyPool/wiki"><strong>Explore the docs »</strong></a>
    <br />
    <br />
    <a href="https://github.com/Aseeef/AseefianProxyPool/issues">Report Bug</a>
    ·
    <a href="https://github.com/Aseeef/AseefianProxyPool/issues">Request Feature</a>
  </p>
</div>



<!-- TABLE OF CONTENTS -->
<details>
  <summary>Table of Contents</summary>
  <ol>
    <li>
      <a href="#about-the-project">About The Project</a>
        <ul>
         <li><a href="#features">Features</a></li>
        </ul>
    </li>
    <li>
      <a href="#getting-started">Getting Started</a>
      <ul>
         <li><a href="#prerequisites">Prerequisites</a></li>
        <li><a href="#installation">Installation</a></li>
      </ul>
    </li>
    <li><a href="#usage">Usage</a></li>
    <li><a href="#license">License</a></li>
    <li><a href="#contact">Contact</a></li>
  </ol>
</details>



<!-- ABOUT THE PROJECT -->
## About The Project

The Aseefian Proxy Pool (APP) is a thread safe framework for managing a large number of proxies. Written to be robust and flexible, the design philosophy of APP is written to give as much power over to the developers as possible. This flexibility lets you as the developer choose exactly how this proxy pool should behave.

### Features

* Built to be thread safe
* Lightweight (with a shaded jar of 36kb)
* Comes with a simple and built in framework for making HTTP requests
* Built in with optional support for the Apache HTTP Client Framework
* Supports using both HTTP and SOCKS5 proxies
* Supports Proxy Authentication
* Ability to "rotate" which proxies from the pool are being used
* Built-in leak detections to analyze which requests are taking longer then they should.
* Built-in proxy health check to automatically remove dead proxies from the pool
* Vast flexibility to give developers full control over the high-level behavior of the pool

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- GETTING STARTED -->
## Getting Started

### Prerequisites

APP requires at least Java 8 or higher.

### Installation

APP maybe installed either via the maven repository jitpack or downloaded directly and then added as a dependency.

#### Gradle
```
repositories {
	...
	maven { url 'https://jitpack.io' }
}
```
```
dependencies {
    implementation ('com.github.Aseeef:AseefianProxyPool:latest.release')
}
```
#### Maven
```
<repositories>
    ...
	<repository>
		 <id>jitpack.io</id>
		 <url>https://jitpack.io</url>
	</repository>
</repositories>
```
```
<dependency>
	<groupId>com.github.Aseeef</groupId>
	<artifactId>AseefianProxyPool</artifactId>
	<version>LATEST</version>
</dependency>
```

<p align="right">(<a href="#readme-top">back to top</a>)</p>



<!-- USAGE EXAMPLES -->
## Usage

```
    // configure the proxy pool's config
    PoolConfig config = new PoolConfig()
            .setProxyTimeoutMillis(3000)
            .setProxyTestFrequency(15000)
            .setConnectionLeakThreshold(30000)
            .setTestProxies(true)
            .setMaxConcurrency(Integer.MAX_VALUE)
            .setLeakTestFrequencyMillis(10)
            .setSortingMode(PoolConfig.SortingMode.LAST_USED);
    // create a list of all proxies that this pool will have
    List<AseefianProxy> proxies = new ArrayList<>();
    proxies.add(new AseefianProxy("host1", 1234, Proxy.Type.HTTP));
    proxies.add(new AseefianProxy("hos2t", 1234, Proxy.Type.HTTP, "username", "password"));
    // create the proxy pool
    pool = new AseefianProxyPool(proxies, config);
    pool.init();

    // now that the pool is created, grab a proxy connection from the pool
    // and execute an http request
    try (ProxyConnection connection = pool.getConnection()) {
        // you don't have to use APP's built in HTTPProxyRequestBuilder.
        // but in this example, I am...
        HTTPProxyRequestBuilder requestBuilder = connection
                .getRequestBuilder("https://some.site/api/v1/request")
                .setHTTPMethod(HTTPProxyRequestBuilder.RequestMethod.POST) // we are making a post reqest
                .setConnectionTimeoutMillis(1000)
                .setContentType(HTTPProxyRequestBuilder.ContentType.APPLICATION_JSON)
                .setContentBody("{\"example\":\"more example\"}");
        HTTPProxyRequest request = requestBuilder.build(); // build the request
        // first attempt to connect to the http server
        request.connect();
        // if there is an issue and the response code is not 200, then do something
        if (request.getResponseCode() != 200) {
            //do something
        }
        // get the response string
        String response = request.getContentString();
        System.out.println(response);
    }
```

_For more examples, please refer to the [Documentation](https://example.com)_

<p align="right">(<a href="#readme-top">back to top</a>)</p>


<!-- LICENSE -->
## License

Distributed under the MIT License. See `LICENSE.txt` for more information.

<p align="right">(<a href="#readme-top">back to top</a>)</p>



<!-- About the author -->
## Contact

Muhammad Aseef Imran -  [contact@aseef.dev](mail:contact@aseef.dev)

Project Link: [https://github.com/Aseeef/AseefianProxyPool](https://github.com/Aseeef/AseefianProxyPool)

<p align="right">(<a href="#readme-top">back to top</a>)</p>


<!-- MARKDOWN LINKS & IMAGES -->
<!-- https://www.markdownguide.org/basic-syntax/#reference-style-links -->
[contributors-shield]: https://img.shields.io/github/contributors/Aseeef/AseefianProxyPool.svg?style=for-the-badge
[contributors-url]: https://github.com/Aseeef/AseefianProxyPool/graphs/contributors
[forks-shield]: https://img.shields.io/github/forks/Aseeef/AseefianProxyPool.svg?style=for-the-badge
[forks-url]: https://github.com/Aseeef/AseefianProxyPool/network/members
[stars-shield]: https://img.shields.io/github/stars/Aseeef/AseefianProxyPool.svg?style=for-the-badge
[stars-url]: https://github.com/Aseeef/AseefianProxyPool/stargazers
[issues-shield]: https://img.shields.io/github/issues/Aseeef/AseefianProxyPool.svg?style=for-the-badge
[issues-url]: https://github.com/Aseeef/AseefianProxyPool/issues
[license-shield]: https://img.shields.io/github/license/Aseeef/AseefianProxyPool.svg?style=for-the-badge
[license-url]: https://github.com/Aseeef/AseefianProxyPool/blob/master/LICENSE.txt
[linkedin-shield]: https://img.shields.io/badge/-LinkedIn-black.svg?style=for-the-badge&logo=linkedin&colorB=555
[linkedin-url]: https://www.linkedin.com/in/aseef/
[jitpack-shield]: https://img.shields.io/jitpack/version/com.github.Aseeef/AseefianProxyPool?style=for-the-badge
[jitpack-url]: https://jitpack.io/#Aseeef/AseefianProxyPool/
[download-shield]: https://img.shields.io/github/downloads/Aseeef/AseefianProxyPool/total?style=for-the-badge
[download-url]: https://jitpack.io/#Aseeef/AseefianProxyPool/