package lunasql.lib.ret;

public class RetPlugin {
   public int ret;
   public int success;
   public String cmdName;

   public RetPlugin(int ret, int success, String cmdName) {
      this.ret = ret;
      this.success = success;
      this.cmdName = cmdName;
   }
}
