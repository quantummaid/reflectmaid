[![Maven Central](https://maven-badges.herokuapp.com/maven-central/de.quantummaid.reflectmaid/reflectmaid-parent/badge.svg)](https://maven-badges.herokuapp.com/maven-central/de.quantummaid.reflectmaid/reflectmaid-parent)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

<img src="quantummaid_logo.png" align="left"/>

# ReflectMaid

An utility project for other QuantumMaid projects that handles reflections. 
 
## GenericType
A lot of configuration methods throughout the QuantumMaid framework and its sub-probjects (HttpMaid, MapMaid, etc.)
require the user to provide a `java.lang.Class` class. E.g., HttpMaid requires usecases to be configured
by providing the `java.lang.Class` object of the use case class.
Due to limitations of the Java Virtual Machine, `java.lang.Class` objects do not sufficiently
support generic types (see [Type Erasure](https://docs.oracle.com/javase/tutorial/java/generics/erasure.html)).

To work around the aforementioned limitation, QuantumMaid offers the `GenericClass` type.
Whenever a configuration method takes a parameter of type `java.lang.Class`, there will be an overloaded variant
of that configuration method that accepts a `GenericType` parameter instead of the `java.lang.Class` parameter.   