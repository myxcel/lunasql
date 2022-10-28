import static org.junit.Assert;
import lunasql.lib.Tools;

public class ToolsTester extends TestCase {

   public ThingTester (String name) {
        super (name);
    }

   public void testGetName() throws Exception {
      assertEquals("a", "b");
   }

   public static void main(String[] args) {
        TestRunner.runAndWait(new TestSuite(ToolsTester.class));
    }
}
