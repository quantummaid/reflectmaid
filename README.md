[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=de.quantummaid.reflectmaid%3Areflectmaid-parent&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=de.quantummaid.reflectmaid%3Areflectmaid-parent)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=de.quantummaid.reflectmaid%3Areflectmaid-parent&metric=reliability_rating)](https://sonarcloud.io/dashboard?id=de.quantummaid.reflectmaid%3Areflectmaid-parent)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=de.quantummaid.reflectmaid%3Areflectmaid-parent&metric=security_rating)](https://sonarcloud.io/dashboard?id=de.quantummaid.reflectmaid%3Areflectmaid-parent)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=de.quantummaid.reflectmaid%3Areflectmaid-parent&metric=alert_status)](https://sonarcloud.io/dashboard?id=de.quantummaid.reflectmaid%3Areflectmaid-parent)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=de.quantummaid.reflectmaid%3Areflectmaid-parent&metric=bugs)](https://sonarcloud.io/dashboard?id=de.quantummaid.reflectmaid%3Areflectmaid-parent)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=de.quantummaid.reflectmaid%3Areflectmaid-parent&metric=code_smells)](https://sonarcloud.io/dashboard?id=de.quantummaid.reflectmaid%3Areflectmaid-parent)
[![Technical Debt](https://sonarcloud.io/api/project_badges/measure?project=de.quantummaid.reflectmaid%3Areflectmaid-parent&metric=sqale_index)](https://sonarcloud.io/dashboard?id=de.quantummaid.reflectmaid%3Areflectmaid-parent)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=de.quantummaid.reflectmaid%3Areflectmaid-parent&metric=coverage)](https://sonarcloud.io/dashboard?id=de.quantummaid.reflectmaid%3Areflectmaid-parent)
[![Last Commit](https://img.shields.io/github/last-commit/quantummaid/reflectmaid)](https://github.com/quantummaid/reflectmaid)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/de.quantummaid.reflectmaid/reflectmaid-parent/badge.svg)](https://maven-badges.herokuapp.com/maven-central/de.quantummaid.reflectmaid/reflectmaid-parent)
[![Code Size](https://img.shields.io/github/languages/code-size/quantummaid/reflectmaid)](https://github.com/quantummaid/reflectmaid)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Slack](https://img.shields.io/badge/chat%20on-Slack-brightgreen)](https://join.slack.com/t/quantummaid/shared_invite/zt-cx5qd605-vG10I~WazfgH9WOnXMzl3Q)
[![Gitter](https://img.shields.io/badge/chat%20on-Gitter-brightgreen)](https://gitter.im/quantum-maid-framework/community)
[![Twitter](https://img.shields.io/twitter/follow/quantummaid)](https://twitter.com/quantummaid)

<img src="quantummaid_logo.png" align="left"/>

# ReflectMaid

A utility project for other [QuantumMaid](https://quantummaid.de/) projects that handles reflections. 
 
## GenericType
A lot of configuration methods throughout the QuantumMaid framework and its sub-probjects (HttpMaid, MapMaid, etc.)
require the user to provide a `java.lang.Class` class. E.g., HttpMaid requires usecases to be configured
by providing the `java.lang.Class` object of the use case class.
Due to limitations of the Java Virtual Machine, `java.lang.Class` objects do not sufficiently
support generic types (see [Type Erasure](https://docs.oracle.com/javase/tutorial/java/generics/erasure.html)).

To work around the aforementioned limitation, QuantumMaid offers the `GenericClass` type.
Whenever a configuration method takes a parameter of type `java.lang.Class`, there will be an overloaded variant
of that configuration method that accepts a `GenericType` parameter instead of the `java.lang.Class` parameter.

## Synthetic methods, constructors and fields
[Synthetic methods, constructors and fields](https://www.baeldung.com/java-synthetic) are ignored by ReflectMaid.
This is recommended [to support tools like JaCoCo](https://www.jacoco.org/jacoco/trunk/doc/faq.html).