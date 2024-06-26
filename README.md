# Fence | [中文](README_ZH.md) [![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html) [![Gitter](https://img.shields.io/badge/ServiceComb-Gitter-ff69b4.svg)](https://gitter.im/ServiceCombUsers/Lobby)

This project is servicecomb-java-chassis security support. The main architecture is based on [The OAuth 2.0 Authorization Framework](https://tools.ietf.org/html/rfc6749) and [OpenID Connect](https://openid.net/connect/). And provides APIs for developers based on [Spring Security](https://spring.io/projects/spring-security). Please see [developers guide](docs/zh_CN/developersGuide.md) for details.

## Authentication diagram

![](docs/authentication.png)


## Authorization diagram

![](docs/authorization.png)

## Project description

This project contains authentication-server, resource server, edge service, admin service and admin website. 

* Prepare

Authentication Server uses MySql database. Install database first, and initialize it by executing authentication-server\src\main\resources\sql\user.sql .

* Build and run

```
build_and_run.bat
```

Can open this file with a text editor to check what it is done.

* Demo web pages

Using admin/changeMyPassword login to the demo pages, and try operations. 
```
http://localhost:9090/ui/admin/
```

* Run tests

After services are started, try
```
cd %HOME%\integration-tests\target
start java -jar integration-tests-0.0.1-SNAPSHOT.jar
```

This test will token several seconds. See implementations of TestCase for testing details.

* Observability

After login, `Cloud Service capapi -> Problem Locating` . Input `Trace-Id` and `大概时间` can search invocation chain logs and related application logs. Using `查看日志`、`查看Metrics` able to download logs and metrics. 

> Tips: Using debug window of browsers can find trace id in HTTP response headers, e.g. `X-B3-Traceid:86560bc39a54d498`.


## Contact Us
* [issues](https://issues.apache.org/jira/browse/SCB)
* mailing list: [subscribe](mailto:dev-subscribe@servicecomb.apache.org) [view](https://lists.apache.org/list.html?dev@servicecomb.apache.org)
