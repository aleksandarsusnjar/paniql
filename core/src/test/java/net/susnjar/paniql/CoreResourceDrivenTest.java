package net.susnjar.paniql;

public abstract class CoreResourceDrivenTest extends ResourceDrivenTest {
    @Override
    public String getResourcePath() {
        return ResourceDrivenTest.getResourcePath(CoreResourceDrivenTest.class);
    }



}
