package ca.ubc.cs.beta.aeatk.misc.csvhelpers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.beust.jcommander.ParameterException;

/**
 * Utility class helpful for parsing CSV Files
 * 
 * NOTE: This class distinguishes between two types of entries
 * data row/column and raw row/column.
 * 
 * Raw IDs are always basically which row and which column.
 * 
 * Data IDs take the specified offsets into account and let the client index the data easier than
 * having to keep track of where the data actually is.
 * 
 * @author sjr
 *
 */
public class ConfigCSVFileHelper implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -9193977861142611700L;
	
	/**
	 * Stores the raw data of the CSV File
	 */
	private final List<String[]> data;
	
	/**
	 * Stores a mapping of keys to the column id
	 */
	private final Map<String, Integer> keyIndexMap =  new HashMap<String, Integer>();
	
	/**
	 * Stored a boolean whether there was a header row or not, but now uses offsets
	 */
	private final boolean header;
	
	/**
	 * Number of rows from the top before the data starts
	 */
	private final int dataOffsetFromTop;
	
	/**
	 * Number of rows from the left before the data stops
	 */
	private final int dataOffsetFromLeft;
	
	
	
	/**
	 * Creates a ConfigCSVFileHelper with the specified data and an implicit header row
	 * @param data list of string arrays, each element of the list is a row, each element of the array a column. This should be a square
	 */
	public ConfigCSVFileHelper(List<String[]> data)
	{
		this(data, true);
	}
	
	/**
	 * Creates a ConfigCSVFileHelper with the specified data
	 * @param data	 list of string arrays, each element of the list is a row, each element of the array a column. This should be a square
	 * @param header <code>true</code> if the first row has keys, <code>false</code> otherwise.
	 */
	public ConfigCSVFileHelper(List<String[]> data, boolean header)
	{
		this(data,(header) ? 1 : 0, 0);
		
	}
	
	/**
	 * Create a ConfigCSVFileHelper with the specified data
	 * 
	 * @param data 					list of string arrays, each element of the list is a row, each element of the array a column. This should be a square
	 * @param dataOffsetFromTop 	number of rows from the top before the data starts 
	 * @param dataOffsetFromLeft	number of columns from the left before the data starts
	 */
	public ConfigCSVFileHelper(List<String[]> data, int dataOffsetFromTop, int dataOffsetFromLeft)
	{
		//Trim all inputs
		for(String[] arr : data)
		{
			for(int i=0; i < arr.length; i++)
			{
				arr[i] = arr[i].trim();
			}
		}
		
		this.data = data;
		this.dataOffsetFromTop = dataOffsetFromTop;
		this.dataOffsetFromLeft = dataOffsetFromLeft;
		
		
		this.header = (dataOffsetFromTop == 1);
		if (data.size() == 0)
		{
			throw new ParameterException("Parameter File empty");
		}
	
		if(this.header)
		{
			String[] keys = data.get(0);
			for(int i=0; i < keys.length; i++)
			{
				keyIndexMap.put(keys[i], i);
			}
			//dataOffsetFromTop = 1;
		} else
		{
			//dataOffsetFromTop = 0;
		}
	
		
	}
	/**
	 * Read a raw integer from the specied cell in the CSV 
	 * @param row raw row id (including headers)
	 * @param column raw column id (including headers)
	 * @return value
	 */
	public int getIntegerRawValue(int row,int column)
	{
		if (data.get(row) == null)
		{
			throw new IllegalArgumentException("Invalid Row");
		}
		
		return Integer.valueOf(data.get(row)[column]);
	}
	
	
	
	/**
	 * Returns an integer for the specified raw row and given column key
	 * @param row 	row id to read from
	 * @param key 	string identifier of the column
	 * @return 	integer value stored in cell
	 */
	public int getIntegerValue( int row,String key)
	{
		
		
		if (keyIndexMap.get(key) == null)
		{
			throw new IllegalArgumentException("Invalid Key");
		}
		
		return getIntegerRawValue(row, keyIndexMap.get(key));
	}
	
	/**
	 * Returns a double valued stored in the raw row and raw column
	 * @param row 		raw row id to read from
	 * @param column	raw column id to read from
	 * @return	double valued stored in cell
	 */
	public double getDoubleRawValue(int row, int column)
	{
		
		if (data.get(row) == null)
		{
			throw new IllegalArgumentException("Invalid Row");
		}
		
		return Double.valueOf(data.get(row)[column]);
		
	}
	
	
	
	/**
	 * Returns the double valued stored in the row with given key
	 * @param row raw row to read from
	 * @param key string representation of column to return
	 * @return double value stored in cell
	 */
	public double getDoubleValue(int row, String key)
	{
		
		if (keyIndexMap.get(key) == null)
		{
			throw new IllegalArgumentException("Invalid Key");
		}
		
		return getDoubleRawValue( row, keyIndexMap.get(key));
	}
	
	
	/**
	 * Returns the string value stored in a given row and key
	 * @param row 	raw row id to read string from
	 * @param key 	string representation of column to read from
	 * @return	string value stored within
	 */
	public String getStringValue(int row, String key)
	{
		if (keyIndexMap.get(key) == null)
		{
			throw new IllegalArgumentException("Invalid Key");
		}
		return getStringRawValue(row,keyIndexMap.get(key));
	}
	
	/**
	 * Returns the string value stored in a given row and column
	 * @param row 		raw row id to read string from
	 * @param column	raw column id to read string from
	 * @return	string valued stored in cell
	 */
	public String getStringRawValue(int row, int column)
	{
		if (data.get(row) == null)
		{
			throw new IllegalArgumentException("Invalid Row");
		}
	
		return data.get(row)[column].trim(); 
		
		
	}
	
	/**
	 * Returns a string value stored in the given data row and data column 
	 * 
	 * You can use this method to have predictable and easy indexing of data
	 * 
	 * @param row 		row to read from, data is indexed from zero
	 * @param column	column to read from, data is indexed from zero
	 * @return	string value stored in cell
	 */
	public String getStringDataValue(int row, int column)
	{
		return getStringRawValue(row + dataOffsetFromTop, column + dataOffsetFromLeft);
	}
	
	/**
	 * Returns a string value stored in the given row with key
	 * @param row 	raw row id to read from
	 * @param key	string representation of column to key
	 * @return	string value stored in cell
	 */
	public String getStringDataValue(int row, String key)
	{
		int column = keyIndexMap.get(key);
		return getStringRawValue(row + dataOffsetFromTop, column);
	}
	
	/**
	 * Reads an integer from the given data row and data column
	 * @param row 		row id to read from, data indexed from zero
	 * @param column	column id to read from, data indexed from zero
	 * @return	integer value stored in cell
	 */
	public Integer getIntegerDataValue(int row, int column)
	{
		return getIntegerRawValue(row + dataOffsetFromTop, column + dataOffsetFromLeft);
	}
	
	/**
	 * Reads an double from the given data row and data column
	 * @param row 		row id to read from, data indexed from zero
	 * @param column	column id to read from, data indexed from zero
	 * @return	double value stored in cell
	 */
	public Double getDoubleDataValue(int row, int column)
	{
		return getDoubleRawValue(row + dataOffsetFromTop, column + dataOffsetFromLeft);
	}
	
	
	
	/**
	 * 
	 * @return index of first data row from top
	 */
	public int getIndexOfFirstData()
	{
		return dataOffsetFromTop;
	}
	
	/**
	 * Returns the number of data rows in the CSV File
	 * @return number of data rows
	 */
	public int getNumberOfDataRows()
	{
		return data.size() - dataOffsetFromTop;
	}
	
	/**
	 * Returns the number of data columns in the CSV File
	 * @return number of data columns
	 */
	public int getNumberOfDataColumns()
	{
		return data.get(0).length - dataOffsetFromLeft;
	}
	
	/**
	 * Returns a column consisting of all the data values in a given column
	 * @param column  Column index to return
	 * @return list of values in string format
	 */
	private List<String> getRawColumn(int column)
	{
		List<String> results = new LinkedList<String>();
	
		for(int i=dataOffsetFromTop; i< data.size(); i++)
		{
			results.add(data.get(i)[column]);
		}
		return results;
	}
	
	
	/**
	 * Gets the id for a given key
	 * @param key string representation of column
	 * @return number giving its offset from the left
	 */
	private int getKeyColumn(String key)
	{
		if (!keyIndexMap.containsKey(key))
		{
			throw new IllegalArgumentException("Invalid Key");
		}
		return keyIndexMap.get(key);
	}
	
	/**
	 * Returns the string data values of a given column 
	 * @param key string representation of the column
	 * @return	list of string data values in the column
	 */
	public List<String> getColumn(String key)
	{
		return getRawColumn(keyIndexMap.get(key));
	}

	/**
	 * Returns the string data values of a given data column
	 * @param column data column to return
	 * @return list of string data values in the column
	 */
	public List<String> getDataColumn(int column)
	{
		return getRawColumn(column + dataOffsetFromLeft);
	}
	/**
	 * Returns a row in array format for a client
	 * 
	 * <b>NOTE:</b> This reference is shared with the CSV file for speed, the client should not modify this.
	 * 
	 * @param index raw row to get
	 * @return string array with all values for a row
	 */
	private String[] get(int index)
	{
		return data.get(index);
	}

	/**
	 * Returns all the entries from a data row (Client can modify this array)
	 * @param index	data row to get
	 * @return 	string array with copies all values for a row 
	 */
	public String[] getDataRow(int index)
	{
		String[] results =  get(index + dataOffsetFromTop);
		return Arrays.copyOfRange(results, dataOffsetFromLeft, results.length);	
	}

	/**
	 * Returns the number of data columns in the CSV File
	 * @param row row id to get columns from
	 * @return number of data columns 
	 */
	public int getNumberOfDataColumns(int row) {
		return get(row).length - dataOffsetFromLeft;
		
	}

	/**
	 * Returns the key for a given data column
	 * @param i index of the data column
	 * @return string representation of the column
	 */
	public String getDataKeyByIndex(int i) {
		return get(0)[i+dataOffsetFromLeft];
		
	}
	
	/**
	 * Returns an array of the IN ORDER listing of keys
	 * @return string array of all the column keys
	 */
	public String[] getDataKeyList()
	{
		List<String> dataList = new ArrayList<String>(get(0).length);
		for(int i =dataOffsetFromLeft; i < get(0).length; i++)
		{
			dataList.add(get(0)[i].trim());
			
		}
		return (String[]) dataList.toArray( new String[0]);
	}

	/**
	 * A list of all the raw row keys starting from the top
	 * @return list of all raw row keys (starting with the first row)
	 */
	public List<String> getRowKeys() {

		return getRawColumn(0);
	}
	
	/**
	 *	Gets a key for a given data column 
	 *	<b>NOTE:</b> This is a duplicate method of {@link ConfigCSVFileHelper#getDataKeyByIndex(int)} 
	 * @param col data column to get the key for
	 * @return string representation of data column
	 */
	public String getKeyForDataColumn(int col)
	{
		return get(0)[col+dataOffsetFromLeft];
	}

	/**
	 * Gets a key for a given data row
	 * 
	 * @param i row to return the key for
	 * @return 	string representation of the row
	 */
	public String getKeyForDataRow(int i) {
		return (get(i+dataOffsetFromTop))[0];
	}
	
	/**
	 * Returns the data column index for a given key
	 * @param key column key to get index of
	 * @return data column index for key
	 */
	public int getDataColumnForKey(String key) {
		return getKeyColumn(key) - dataOffsetFromLeft;
		
	}


	
	
}
