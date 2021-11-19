

public class Secret
{
    private String name;
    private final boolean flag;
    
    public Secret(String name, boolean flag)
    {
	this.name = name;
	this.flag = flag;
    }
    
    public boolean getFlag()
    {
	return this.flag;
    }
    
    public String getName()
    {
	return this.name;
    }
    
    public void setName(String name)
    {
	this.name = name;
    }
}
