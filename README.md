# Paniql: GraphQL Request Costing 

> **Status**: This is an early experimental alpha version designed to gauge interest
> and showcase the possibilities. Please reach out if you are interested in the approach, 
> its direct application, or its development. Minimal tests are running, as the project 
> goals are still evolving. ***Feedback and assistance are welcome.***

## Goal

Help distinguish between normal and malicious use of GraphQL APIs
of any size and complexity.

## Non-goal

The aim is not to accurately predict or even estimate performance, time taken,
or resource utilization for GraphQL queries, as these should not have an impact
on what valid API uses are. It is understood that this project could be used for
some related request analysis but, at this time, this is not the primary goal.

## Background

API security teams got used to various combinations of rate and endpoint constraints
with REST and other http-based APIs. The model of the world these approaches use matches
the reality of use cases only as much as underlying APIs do. When singular real-world
use cases translate to many requests and/or endpoints the mapping becomes broken. 
Distinguishing logic begins to operate on secondary, derivative information, without the
use case context. This yields compromises being made that affect the recognition quality.

Despite this many attempt to apply the same thinking to GraphQL APIs, presumably because
that is what they are familiar with and/or have the tools for. That is a failure in the
following ways:

- GraphQL requests are dynamic so a single request to a single endpoint can be an issue.
- Not leveraging better alignment of GraphQL to use cases (closer to one request per case).

This lead to the spread of blind GraphQL request depth restrictions, another failure:

- Even shallow GraphQL requests can be "dangerous" using batching, especially with fragments.
- Even deep GraphQL requests can be perfectly safe, e.g. navigating "to one" relations.

More recently, query/request complexity accounting started popping up, in a variety of 
ways. These are better but, at least in some cases, appear to be rudimentary and/or
force an oversimplification that replaces one kind of secondary/derivative information
(e.g. REST API request rate) with another, only slightly better single number, without
accounting for resource type access constraints.

## Approach

Paniql analyses a given GraphQL request and, given the GraphQL schema annotated with
[Paniql directives](core/src/main/resources/net/susnjar/paniql/PaniqlSchema.graphqls),
estimates the access and a number of different standard work type quantities for each
type and field, while supporting servers able to optimize queries by "looking ahead"
(see [1](https://www.graphql-java.com/blog/deep-dive-data-fetcher-results/),
[2](https://www.apollographql.com/blog/backend/performance/optimizing-your-graphql-request-waterfalls/),
[3](https://engineering.zalando.com/posts/2021/03/optimize-graphql-server-with-lookaheads.html),
[4](https://www.graphile.org/graphile-build/look-ahead/),
[5](https://boopathi.blog/graphql-optimization-lookaheads), 
... for commentaries, variety of programming languages),
These quantities are standardized and not rolled-up. 
Instead, they are all independently available.

Have a look at [some report examples](print/doc/samples) and 
[corresponding sample schema and requests](core/src/test/resources/net/susnjar/paniql/) for illustration.
Paniql is meant to allow us to analyse queries such as:

```GraphQL
{ 
  folder(id: 123) {
    name
    files {
      id 
      name
    }
  }
}
```
and deduce something like t:

- At most 1 `Folder` instance will be accessed
 
- At most 1 `Folder.name` will be accessed.
 
- Between 0 and (say) 1000 `File` instances will be accessed, with some probability distribution.

- Equal number of `File.id` and `File.name` fields will be accessed.

- (Possibly) `File.id` and `File.name` may be very efficiently fetched in bulk, together with the containing `Folder`.

Reasoning behind avoiding concrete cost units or custom work types is that in 
multi-service environments it becomes almost impossible  to synchronize different 
standards and/or perform what would be the equivalent of "(cost) currency conversion".
This is in addition to focusing on primary, first-order  information mapped from the 
use cases and away from the [non-goal](#non-goal) - it isn't about the performance or
resource utilization but the validity of the usage.

## Implied but left out

Paniql is not a complete solution, only a part of it. It only analyses the requests
and does not make any decisions. Actual decision-making and related constraints are
(presently) not handled by Paniql. 

Recommended approach is to use Paniql together with a sliding time window quota
system that can additionally have distinct quotas for different effective users
(authenticated and/or not), client types (if known, e.g. which application/agent
is making requests), client network locations (if known) and paths that requests
took to arrive (e.g. which network interface and/or gateway). Since Paniql exposes
many distinct metrics, the quotas would express different constrains for each 
metric of interest.

For example, any combination of the following could be envisioned as quotas:

- Unauthenticated users have "0 per request" quotas on fields or types requiring 
  authentication.

- Unauthenticated users have certain quotas on authentication/login operations.

- Different applications (client types) have "0 per request" quotas on operations they are  not intended or built for. Those would imply "counterfeit" clients.

- Different applications and users can have appropriate and distinct quotas that would meaningfully let them to their jobs and not more simply because they themselves are not expected to be able to handle more.

- Multiple separate time window lengths can be specified, e.g. per second, per minute, per 15 minutes, per hour, per 8 hours, per day, such that quotas for longer windows are less than corresponding multiples of quotas of smaller windows.

Such quota/user tracking logic could also be present in or added to a variety of
existing API gateways. Additionally, as a Java project, Paniql could be used within
Java GraphQL projects as an embedded first level of defence against malicious
requests. Note that, while current implementation is in Java, this is not meant to be
tied to Java at all.

## Project structure

Though small, the project is divided into three modules:

- [core](core/) - the code expected to be needed in production.
  Main classes of interest to start with are:
    - [Environment](core/src/main/java/net/susnjar/paniql/Environment.java) - reusable representation the annotated GraphQL API schema model.
    - [Request](core/src/main/java/net/susnjar/paniql/Request.java) - representation of the GraphQL request to analyse.
    - [Invoice](core/src/main/java/net/susnjar/paniql/pricing/Invoice.java) - the result of the analysis.
- [print](print/) - the [code](print/src/main/java/net/susnjar/paniql/print/InvoicePrinter.java) that produces formatted ASCII text reports of [Invoice(s)](core/src/main/java/net/susnjar/paniql/pricing/Invoice.java).
- [commandline](commandline) - a command-line utility that utilizes the other two projects to allow "playing" with Paniql from command line.

Documentation is pretty limited at the moment. Please see the test classes for code examples:

- [Basic use](core/src/test/java/net/susnjar/paniql/InvoiceTest.java)
- [Going over results, printing](print/src/test/java/net/susnjar/paniql/print/InvoicePrinterTest.java)

## To do

- Get+apply feedback + help
- Finish CI+CD
- More documentation
- Fix bugs
- Figure out what else to do, if there's interest