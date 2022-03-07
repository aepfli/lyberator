package at.schrottner.lyberator.resolving

import groovy.json.JsonBuilder
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

import javax.inject.Inject

class ResolverTask extends DefaultTask {

	@Internal
	ResolverExtension extension

	Configuration configuration

	@Inject
	ResolverTask(config, extension) {
		this.extension = extension
		this.configuration = config
		this.description = "extract all CommerceExtensions"
		this.group = "Commerce Extensions"
	}

	@InputFiles
	def getConfiguration() {
		return configuration
	}

	@Input
	def getInput() {
		configuration.dependencies.collect {
			it.toString()
		}
	}
	@OutputFile
	def getDependencies () {
		project.file("$project.buildDir/commerce/extensions.json")
	}

	@TaskAction
	void performAction() {
		def configPoms = project.configurations.maybeCreate(ResolverExtension.CONFIGURATION_EXTENSION_POMS)
		def configZips = project.configurations.maybeCreate(ResolverExtension.CONFIGURATION_EXTENSION_ZIPS)


		configuration.resolvedConfiguration.lenientConfiguration.allModuleDependencies.each { dependency ->

			if(dependency.moduleArtifacts.every { it.file == null} ) {
				project.logger.info "Evaluating dependency $dependency.name as we could not resolve a file, so we assume it is a Commerce Extension"
				project.dependencies {
					"${configPoms.name}"(
							group: dependency.moduleGroup,
							name: dependency.moduleName,
							version: dependency.moduleVersion,
							ext: 'pom'
					)
					"${configZips.name}"(
							group: dependency.moduleGroup,
							name: dependency.moduleName,
							version: dependency.moduleVersion,
							ext: 'zip'
					)
				}
			}
		}

		Map<String, CommerceDep> oldExt

		if(dependencies.exists()) {
			oldExt = new JsonSlurper().parse(dependencies) as Map<String, CommerceDep>
			dependencies.delete()
		} else {
			oldExt = [:]
		}

		Map<String, CommerceDep> pathLookUp = configPoms.resolvedConfiguration.getResolvedArtifacts().collectEntries {
			def parsedPom = new groovy.xml.XmlSlurper().parse(it.file)
			def path
			parsedPom.properties."project.path".each {
				path = it
			}
			CommerceDep dep = new CommerceDep()
			dep.path = path
			dep.group = parsedPom.groupId
			dep.name = parsedPom.artifactId
			dep.version = parsedPom.version
			oldExt.remove(it.id.componentIdentifier.toString())
			[it.id.componentIdentifier.toString(), dep]
		}


		project.logger.info "Deleting unused extensions ${oldExt.entrySet().size()}"
		// delete old dead extensions
		oldExt.values().each {
			project.delete("hybris/bin/${it.path?: "thirdParty"}")
			project.logger.info "Deleting $it.name ($it.path) as it is no more part of the dependencies"
		}

		dependencies << new JsonBuilder(pathLookUp).toPrettyString()
		def fileLookUp = configZips.resolvedConfiguration.getResolvedArtifacts().collectEntries { artifact ->
			[artifact.id.componentIdentifier.toString(), artifact.file]
		}

		fileLookUp.entrySet().each { entry ->
			def target = project.mkdir("hybris/bin/${pathLookUp.get(entry.key).path?: "thirdParty"}")
			project.copy {
				from project.zipTree(entry.value)
				into target
			}
		}
	}

	class CommerceDep {
		String name
		String group
		String version
		String path
	}
}
