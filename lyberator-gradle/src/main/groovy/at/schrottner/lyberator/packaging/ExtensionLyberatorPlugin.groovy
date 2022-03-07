package at.schrottner.lyberator.packaging

import groovy.xml.XmlParser
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Attribute
import org.gradle.api.component.SoftwareComponentFactory
import org.gradle.api.distribution.DistributionContainer
import org.gradle.api.distribution.plugins.DistributionPlugin
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin

import javax.inject.Inject

class ExtensionLyberatorPlugin implements Plugin<Project> {

	public static final String FALLBACK_GROUP = "de.hybris"
	private final SoftwareComponentFactory softwareComponentFactory

	@Inject
	ExtensionLyberatorPlugin(SoftwareComponentFactory softwareComponentFactory) {
		this.softwareComponentFactory = softwareComponentFactory
	}

	@Override
	void apply(Project project) {
		def extension = project.extensions.create(LyberatorExtension.NAME, LyberatorExtension)
		extension.extractionPoint = project.extensions.extraProperties.get("extractionPoint")
		extension.target = project.extensions.extraProperties.get("target")

		def commerceConfig = project.configurations.maybeCreate(LyberatorExtension.CONFIGURATION_NAME)
		def myAttribute = Attribute.of("type", String)
		commerceConfig.attributes {
			attribute(myAttribute, 'zip')
		}

		def extFile = project.file(LyberatorExtension.EXTENSION_XML)
		if (extFile.exists()) {
			def extensioninfo = new XmlParser().parse(extFile)
			//project.name = extensioninfo.extension.@name

			project.group = extensioninfo.extension.coremodule.@packageroot[0].toString() - ".$project.name"
			commerceConfig.dependencies.add(
					project.dependencies.create(project.project(":platform"))
			)
			extensioninfo.extension."requires-extension".each {
				commerceConfig.dependencies.add(
						project.dependencies.create(project.project(":${it.@name}"))
				)
			}
		} else {
			project.group = FALLBACK_GROUP
		}

		project.pluginManager.apply DistributionPlugin
		DistributionContainer distributions = project.extensions.getByName("distributions") as DistributionContainer

		def dist = distributions.maybeCreate('main')
		dist.contents({
			from project.projectDir
			exclude("build/")
			into ("/")
		})
		// create an adhoc component
		def adhocComponent = softwareComponentFactory.adhoc(LyberatorExtension.CONFIGURATION_NAME)
		// add it to the list of components that this project declares
		project.components.add(adhocComponent)
		// and register a variant for publication
		adhocComponent.addVariantsFromConfiguration(commerceConfig) {
			it.mapToMavenScope("compile")
		}
		project.pluginManager.apply MavenPublishPlugin
		PublishingExtension publishing = project.extensions.getByName("publishing") as PublishingExtension
		publishing.publications.register(LyberatorExtension.CONFIGURATION_NAME, MavenPublication.class, {

			from adhocComponent
			artifact project.tasks.getByName("distZip")

			pom {
				properties = [
						"project.path": project.rootProject.file("$extension.extractionPoint/hybris/bin").relativePath(project.projectDir)
				]
			}
		})


	}
}
