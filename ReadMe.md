# L[y]berator

An alternative approach to SAP Commerce Setup.

Normally you extract a zip locally, and remove unused extensions.
We try to swap this approach by centralizing it.

First we extract the SAP Commerce zip and repackage it into separate extension with proper dependencies to other extensions within the POM-file.

Second we are loading only our needed extensions via Gradle dependency mechanisms and place them back in the correct folder.

## DISCLAIMER

You have to evaluate on your own, if you want to temper with Software Package you do not own.
This is just Prototype, a PoC showing the possibilities.

I repeat, I am not telling you to use this, and all kind of alterations, modifications of Software Distributions is happening on your own risk.

## Testing this PoC

1. check it out.
2. run `gradle publishToMavenLocal` - this will build the plugins etc
3. repackage something with the `testing/extraction` project
4. use the packaged resources see `testing/usage` project

## Repackaging

First we unzip the SAP Commerce Distribution.

Second we go through all the file hierarchy and search for `extensioninfo.xml`.
We parse the `extensioninfo.xml` to find the groupId and the extension name.
Furthermore, we use the `required-extensions` to define dependencies between the extensions.
We create own gradle subprojects out of those extensions found, and publish them via maven.
Additionally, we enrich the pom file with a custom attribute providing the path, for the extraction in the future.

### Usage

Take a look at `testing/extraction`. This is a ready to use gradle-plugin, you only need to define the zip file via the `target` variable.
Either within `gradle.properties` or with `-Ptarget=`

## Using repackaged files

As we are now able to reference the extensions within our build.gradle file, we are fetching those.
Furthermore, we do fetch all dependencies based on the pom configurations.
We use this information to create additional `zip`-extension dependencies to the same coordinates.

```groovy
dependencies {
	commerceExtension "<coordinates>"
}
```

## TODOS

### Usage and definition of custom and third party libraries

How can we programmatically differentiate between them.
This sounds like it is not needed, but as soon as you want to use this information for additional tooling, like spotless or sonarqube this is needed.

### define proper sourcesets and libs

We could define proper sourcesets for all the extensions, based on the `ant gradle` outcome of SAP Commerce.
This would allow us to generate a fully flexible build for our Commerce Extensions.

Anyways we need this for our custom/local extensions to use them for analyzers, test execution and linters.

### differentiation between commerceExtension and customExtension

commerceExtensions are not needed for ccv2 whereas customExtensions would be needed.

### add external-dependencies.xml also to pom file

// TODO theoretically we could do this only for dependencies which do not resolve to files (this way we could try to incorporate the `external-dependencies.xml`)

### Evaluate platform

Currently, platform is a dependency for all SAP Commerce extensions.
Do we need that?
Are the extensions within platform own extensions, or not?
