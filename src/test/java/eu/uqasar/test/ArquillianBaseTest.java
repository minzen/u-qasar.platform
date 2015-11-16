package eu.uqasar.test;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 *
 */

/**
 *
 *
 */
public abstract class ArquillianBaseTest {

	protected static final String REST_BASE_URL = "http://localhost:8080/foobar9-test";

	@Deployment(managed = true)
	public static WebArchive createJBoss7TestArchive() {
		WebArchive webArchive = null;
//		File[] libs = Maven.resolver().loadPomFromFile("pom.xml").importRuntimeAndTestDependencies().resolve().withTransitivity().asFile();
//		String basePackage = "eu.uqasar";
//		basePackage.replaceAll(".", "/");
//		webArchive = ShrinkWrap
//				.create(WebArchive.class, "foobar9-test.war")
//				.addPackages(true, basePackage + "/interceptor",
//						basePackage + "/model", basePackage + "/qualifier",
//						basePackage + "/rest", basePackage + "/service",
//						basePackage + "/util")
//				.addClass(ArquillianBaseTest.class)
//				// add a custom datasource definition for testing as to not
//				// interfere with normal deployment's database
//				.addAsWebInfResource("test-ds.xml")
//				.addAsWebInfResource("test-persistence.xml",
//						"classes/META-INF/persistence.xml")
//				.addAsWebInfResource("test-beans.xml", "beans.xml")
//				.addAsLibraries(libs);
		return webArchive;
	}

}