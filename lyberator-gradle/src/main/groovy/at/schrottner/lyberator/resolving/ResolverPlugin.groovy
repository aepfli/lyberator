package at.schrottner.lyberator.resolving

import at.schrottner.lyberator.packaging.LyberatorExtension
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

class ResolverPlugin implements Plugin<Project> {


	@Override
	void apply(Project project) {
		def extension = project.extensions.create(ResolverExtension.NAME, ResolverExtension)


		def config = project.configurations.maybeCreate(ResolverExtension.CONFIGURATION_EXTENSION)


		def task = project.tasks.create("extractCommerce", ResolverTask, config, extension)

	}
}
