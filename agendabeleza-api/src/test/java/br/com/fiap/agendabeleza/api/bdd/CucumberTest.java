package br.com.fiap.agendabeleza.api.bdd;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

/**
 * Suíte BDD: roda as features de {@code src/test/resources/features} com o Cucumber no JUnit Platform. O nome
 * termina em {@code Test} para o Surefire executá-la no {@code verify}, inclusive no CI.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "br.com.fiap.agendabeleza.api.bdd")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME,
		value = "pretty, html:target/cucumber-reports/cucumber.html, json:target/cucumber-reports/cucumber.json")
class CucumberTest {
}
