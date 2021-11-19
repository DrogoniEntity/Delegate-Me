# Delegate-Me
An attempt to create dynamic delegation classes.

## What is it ?
This project aim to create new proxy classes which they will be used for delegation.
For example, you can delegate an `java.lang.String` to trace any invocations.

## How to use it ?

### Prerequisite :
To use this library, you will need to install :
- [Byte-Buddy](https://github.com/raphw/byte-buddy) (version 1.12.1 or later)
- [Objenesis](http://objenesis.org/) (version 3.2 or later)

### Writing a delegation class
You will need to create a new class which extends `Delegator<T>` (`fr.drogonistudio.delegateme.Delegator`), the type `<T>` is your
object's type to delegate. Then, you will implement the following method: 
```java
public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
```
This method will be invoked everytime a public method is invoked in your proxied object. In this method, you can use :
```java
protected final Object delegate(Object proxy, Method method, Object[] args) throws Throwable
```
to perform delegation.

### Generating delegated object
Once you complete your `Delegator`, you can warp your object to delegate with `DelegatorFactorty.warp(Class<T>, Delegator<T>)`. The returned
object will be a clone a delegated object (every fields values are copied during generation process) but with the difference which any public
methods will invoke your `Delegator`'s `invoke` method.

You can find examples into `example` directory.
