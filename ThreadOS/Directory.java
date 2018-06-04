/* Directory
 * Author: Michael Rossiytsev
 * Date: 5/30/18
 * Description: Holds information about filenames and the filenames size.
 *              The filesize array is used to make searching simple.
 *              Capable of being serialized into a byte[] data to be written
 *              to a block on DISK, and deserialization. 
 *
 * Illustration of a newly instantiated Directory
 *  inode    fsize   fname   
 *    0        0     ['/']
 *    1        -       -
 *    :        -       - 
 *   max       -       -
*/
public class Directory {
    private static int maxChars = 30;   // Size of file name

    // Directory entries
    private int fsize[];        // each element stores a files name size
    private char fnames[][];    // each element stores a different file name

    // Constructor:
    // Preconditions: maxInumber = max number of files/inodes
    // Postconditions:
    public Directory(int maxInumber) {
    	//SysLib.cout("initialized Directory\n");    
    	
    	// Instantiate fsize[] and fnames[][]
    	fsize = new int[maxInumber];
    	for(int i = 0; i < fsize.length; i++){
    		fsize[i] = 0;
    	}
		fnames = new char[maxInumber][maxChars];
		
		// Set root directory name and size
		String root = "/";
		fsize[0] = root.length();
		root.getChars(0, fsize[0], fnames[0], 0);
		
		//printData();
    }


    // bytes2directory: 
    // Preconditions: Assumes data[] has been saved in the same way that 
    //                directory2bytes() formats the byte[]
    // Postconditions: Structures the directory based on data[]
    //                 Populates fsize and fnames.
    public void bytes2directory(byte data[]){
      	//SysLib.cout("Directory...bytes2directory...\n");
      	int index = 0;
      	
      	// Populate fsize[]
		for(int i = 0; i < fsize.length; i++, index += 4){
			fsize[i] = SysLib.bytes2int(data, index);
		}
		
		// Populate fnames[][]		
		for(int i = 0; i < fsize.length; i++){
			for(int j = 0; j < fsize[i] && fsize[i] != 0; j++, index += 2){
				fnames[i][j] = (char)SysLib.bytes2short(data, index);
			}			
		}
    }


    // directory2bytes: Convert directory structures and data members into
    //                  a serialized byte array.
    // Preconditions: Assuming this byte array is written to disk elsewhere.
    // Postconditions: Returns a byte[] that holds converted Directory data.
    public byte[] directory2bytes(){
       	//SysLib.cout("Directory...directory2bytes...\n");
		
		// Data for conversion
		int index = 0;
		int dataLength = (fsize.length * 4) + ((fnames.length * maxChars) * 2);
		byte[] data = new byte[dataLength];
		
		// Convert fsize[] to bytes
		for(int i = 0; i < fsize.length; i++, index += 4){
			SysLib.int2bytes(fsize[i], data, index); 
		}
		
		// Convert fnames[][] to bytes
		for(int i = 0; i < fnames.length; i++){
			for(int j = 0; j < fsize[i]; j++, index += 2){
				SysLib.short2bytes((short)fnames[i][j], data, index);
			}
		}
		
		return data;
    }


    // ialloc: Attempts to find an entry in the directory that has the same
    //         filename as the argument provided.
    // Preconditions:
    // Postconditions: Return -1 if there was no free slot for the filename 
    //                 to be allocated to. Otherwise, assign the slot
    //                 in fname to filename as a char[] and return the inumber.
    public short ialloc(String filename){
		//SysLib.cout("Directory...ialloc...\n");
		short i = 0;
		for(i = 0; i < fnames.length && fsize[i] > 0; i++){ }
		
		if(i >= fnames.length){	// Index is out of bounds. No free slot.
			return -1;
		} else {
			fsize[i] = filename.length();	
			//SysLib.cout("fsize["+ i + "]:" + fsize[i] + "\n");					
			filename.getChars(0, fsize[i], fnames[i], 0);
			return i;
		}
    }


    // ifree: Deallocating the entry in the directory based on the inumber/index
    // Preconditions:
    // Postconditions: Reinstantiates the char[] located at the specific 
    //                 iNumber argument. The size is altered to -1 as well.
    public boolean ifree(short iNumber){
    	//SysLib.cout("Directory...ifree...\n");

		// Continue only if iNumber is valid
		if(iNumber > 0 && iNumber < fsize.length){
			fsize[iNumber] = 0;
			fnames[iNumber] = new char[maxChars];
			return true;
		} else {
			return false;
		}
    }


    // namei: Essentially and indexOf()
    // Preconditions:
    // Postconditions: Returns the index of the requested filename. 
    //                 Returns -1 if the filename was not found in directory.
    public short namei(String filename){
       	//SysLib.cout("Directory...namei...\n");
       	short i;
      	
		for(i = 0; i < fsize.length && !(new String(fnames[i])).equals(filename); i++){
		       	String t = new String(fnames[i], 0, fsize[i]);	       	
		       	//SysLib.cout("t:" + t + "....filename:" + filename + "\n");
		       	if(t.equals(filename)){
		       		return i;
		       	}		       	 
		}

		return -1;
    }
    
    
    //=============================HELPERS======================
	// Test Helper method
	// Preconditions:
	// Postconditions: Prints out fsize and fnames 
	public void printData() {
	    SysLib.cout("fsize         fnames\n");
		for(int i = 0; i < fsize.length; i++){
			SysLib.cout(fsize[i] + "               ");
			for(int j = 0; j < fnames[i].length; j++){
				SysLib.cout(fnames[i][j] + "");
			}
			SysLib.cout("\n");
		}
	}
    
    
}
