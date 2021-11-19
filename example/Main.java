import java.util.Random;

import fr.drogonistudio.delegateme.Delegator.EqualsCompareStrategy;
import fr.drogonistudio.delegateme.DelegatorFactory;

public class Main
{
    
    public static void main(String args[]) throws Exception
    {
	{
	    IntWrapper wrapper = new IntWrapper();
	    System.out.println("Base value: " + wrapper.value);
	    
	    wrapper.value = 10;
	    System.out.println("My value: " + wrapper.value);
	    
	    System.out.println("Creating proxied...");
	    IntWrapper delegated = DelegatorFactory.warp(IntWrapper.class, new InvocationTracer<>(wrapper));
	    System.out.println("Created");
	    
	    delegated.setValue(5);
	    delegated.getValue();
	    
	    System.out.println("Now, I've got " + wrapper.value);
	    
	    delegated.toString();
	    delegated.hashCode();
	    delegated.equals(wrapper);
	    
	    wrapper.value = 123;
	    DelegatorFactory.getDelegator(delegated).updateProxyFieldsValue(delegated, EqualsCompareStrategy.BY_EQUALS_METHOD);
	    System.out.println("### Delegated fields' value: ");
	    System.out.println("- value: " + delegated.value);
	    System.out.println("- initialValue: " + delegated.initialValue);
	    
	    delegated.value = 456;
	    DelegatorFactory.getDelegator(delegated).updateDelegatedFieldsValue(delegated, EqualsCompareStrategy.BY_EQUALS_METHOD);
	    System.out.println("### Base object value: ");
	    System.out.println("- value: " + wrapper.value);
	    System.out.println("- initialValue: " + wrapper.initialValue);
	}
	
	for (int i = 0; i < 10; i++)
	    System.out.println();
	
	{
	    IntWrapper iw1 = new IntWrapper(1);
	    IntWrapper iw2 = new IntWrapper(2);
	    
	    IntWrapper d1 = DelegatorFactory.warp(IntWrapper.class, new InvocationTracer<>(iw1));
	    IntWrapper d2 = DelegatorFactory.warp(IntWrapper.class, new InvocationTracer<>(iw2));
	    
	    d1.getValue();
	    d2.getValue();
	    
	    d1.setValue(100);
	    d2.setValue(-200);
	}
	
	for (int i = 0; i < 10; i++)
	    System.out.println();
	
	{
	    Secret object = new Secret("ABC", true);
	    InvocationCounter<Secret> delegator = new InvocationCounter<>(object);
	    
	    Secret proxied = DelegatorFactory.warp(Secret.class, delegator);
	    System.out.println("Flag set to " + proxied.getFlag());
	    proxied.setName("DEF");
	    proxied.setName("XYZ");
	    
	    delegator.printCounter(System.out);
	}
    }
    
    static class IntWrapper
    {
	private final int initialValue;
	private int value;
	
	public IntWrapper()
	{
	    this.value = new Random().nextInt();
	    this.initialValue = this.value;
	}
	
	public IntWrapper(int value)
	{
	    this.value = value;
	    this.initialValue = value;
	}
	
	public int getValue()
	{
	    return this.value;
	}
	
	public void setValue(int value)
	{
	    this.synchronizedSetValue(value);
	}
	
	private void synchronizedSetValue(int value)
	{
	    synchronized (this)
	    {
		this.value = value;
	    }
	}
	
	@Override
	public String toString()
	{
	    return "Wrapped " + this.value;
	}
    }
}
