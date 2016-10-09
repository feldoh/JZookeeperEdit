package net.imagini.zkcli;

import net.imagini.zkcli.CliParameters;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class CliParametersTest {

    private CliParameters underTest;

    @Test
    public void testIncludesAction() {
        underTest = new CliParameters(new String[] { "--rm-recursive" });
        Assert.assertTrue(underTest.includesAction());

        underTest = new CliParameters(new String[] { "--rm" });
        Assert.assertTrue(underTest.includesAction());

        underTest = new CliParameters(new String[] { "--rm-children" });
        Assert.assertTrue(underTest.includesAction());

        underTest = new CliParameters(new String[] { "-p" });
        Assert.assertFalse(underTest.includesAction());
    }
}
