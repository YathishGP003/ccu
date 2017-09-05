package a75f.io.dal;

import com.google.api.client.util.Key;

/**
 * Created by Yinten on 9/4/2017.
 */

public class CCUUser
{
	@Key
	private String username;
	@Key
	private String password;
	
	
	public String getUsername()
	{
		return username;
	}
	
	
	public void setUsername(String username)
	{
		this.username = username;
	}
	
	
	public String getPassword()
	{
		return password;
	}
	
	
	public void setPassword(String password)
	{
		this.password = password;
	}
}
