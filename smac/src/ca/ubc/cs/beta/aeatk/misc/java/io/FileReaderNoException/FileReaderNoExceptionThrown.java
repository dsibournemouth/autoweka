package ca.ubc.cs.beta.aeatk.misc.java.io.FileReaderNoException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

/**
 * File Reader object that doesn't throw FileNotFound in it's constructor
 * 
 * 
 * @author Steve Ramage 
 *
 */
public class FileReaderNoExceptionThrown extends Reader
{
	private final FileReader fileReader;  
	public FileReaderNoExceptionThrown(File f)
	{
		try {
			this.fileReader = new FileReader(f);
		} catch (FileNotFoundException e) {

			throw new IllegalStateException(e);
		}
	}
	
	@Override
	public boolean ready() throws IOException
	{
		return fileReader.ready();
	}
	
	@Override
	public int read() throws IOException {
		return fileReader.read();
	}
	@Override
	public int read(char[] cbuf) throws IOException {
		return fileReader.read(cbuf);
	}
	@Override
	public int read(char[] cbuf, int off, int len) throws IOException {

		return fileReader.read(cbuf, off, len);
	}
	@Override
	public void close() throws IOException {
		fileReader.close();
	}
	
	@Override
	public boolean markSupported()
	{
		return fileReader.markSupported();
	}
	
	@Override
	public void mark(int readAheadLimit) throws IOException
	{
		 fileReader.mark(readAheadLimit);
	}
	@Override
	public void reset() throws IOException
	{
		fileReader.reset();
	}
	
	public long skip(long n) throws IOException
	{
		return fileReader.skip(n);
	}
}