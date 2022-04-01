package at.schrottner.lyberator.packaging

import groovy.ant.AntBuilder
import groovy.io.FileType
import groovy.xml.XmlParser
import org.gradle.api.Plugin
import org.gradle.api.component.SoftwareComponentFactory
import org.gradle.api.initialization.ProjectDescriptor
import org.gradle.api.initialization.Settings

import javax.inject.Inject

class LyberatorPlugin implements Plugin<Settings> {

	private final SoftwareComponentFactory softwareComponentFactory

	@Inject
	LyberatorPlugin(SoftwareComponentFactory softwareComponentFactory) {
		this.softwareComponentFactory = softwareComponentFactory
	}

	@Override
	void apply(Settings settings) {

		def extension = settings.extensions.create(LyberatorExtension.NAME, LyberatorExtension)
		extension.extractionPoint = settings.extensions.extraProperties.get("extractionPoint")
		extension.target = settings.extensions.extraProperties.get("target")

		def rootDir = settings.rootDir
		File extractionPoint = new File("${rootDir}/${extension.extractionPoint}")
		if(!extractionPoint.exists() && extension.target) {
			extractionPoint.mkdir()
			def ant = new AntBuilder()
			ant.unzip(
					src: "${rootDir}/${extension.target}",
					dest: extractionPoint.path,
					overwrite: true
			)
		}

		def bomRoot = createBomProject(extractionPoint, settings)
		FilenameFilter filter = (dir, name) -> name == LyberatorExtension.EXTENSION_XML
		rootDir.traverse(type: FileType.DIRECTORIES) {
			def projectName
			if(it.listFiles(filter)) {
				def extensioninfo = new XmlParser().parse("$it.absolutePath/$LyberatorExtension.EXTENSION_XML")
				projectName = extensioninfo.extension[0].@name
			} else if(it.path.endsWith("bin${File.separator}platform")) {
				projectName = it.name
			}

			if(projectName) {
				settings.include projectName
				def p = settings.project(":${projectName}")
				p.projectDir = it
			}
		}
	}

	def createBomProject(File extractionPoint, Settings settings) {
		def projectName = "commerce-bom"
		def bomRoot = new File("$extractionPoint/$projectName")
		bomRoot.mkdirs()

		settings.include projectName

		def p = settings.project(":${projectName}")
		p.projectDir = bomRoot
		return p
	}
}
