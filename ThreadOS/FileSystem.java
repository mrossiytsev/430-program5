public class FileSystem {
    private SuperBlock superblock;
    private Directory directory;
    private FileTable filetable;

    private final int SEEK_SET = 0;
    private final int SEEK_CUR = 1;
    private final int SEEK_END = 2;

    public FileSystem(int diskBlocks){
        superblock = new SuperBlock(diskBlocks);
        directory = new Directory(superblock.inodeBlocks);
        filetable = new FileTable(directory);

        //FileTableEntry dirEnt = open("/", "r");
        //int dirSize = fsize(dirEnt);
        //if(dirSize > 0){
        //    byte[] dirData = new byte[dirSize];
        //    read(dirEnt, dirData);
        //    directory.bytes2directory(dirData);
        //} 
        //close(dirEnt);
    }

    // format----------------------------
    public boolean format(int files){
        if(files < 0) return false;

        directory = new Directory(files);
        filetable = new FileTable(directory);

        superblock.totalBlocks = 1000;
        superblock.format(files);

        return true;
    }

    // open------------------------------
    public FileTableEntry open(String filename, String mode){
        return filetable.falloc(filename, mode);
    }

    // close------------------------------
    public boolean close(FileTableEntry entry){
            return filetable.ffree(entry);
    }

    // write-------------------------------
    // DOESN'T CHECK FOR INDIRECT POINTER BLOCKS
    public int write(FileTableEntry ftEnt, byte[] buffer){
        // Check for validity
        if(buffer == null || ftEnt == null) return -1;
        
        // Get inode
        Inode inode = ftEnt.inode;

        // Get current position
        int seek = ftEnt.seekPtr;
        int block = (seek/512); // Index to direct[]. The LOGICAL Block
        
        // Get enough blocks for the buffer
        int blocksForBuffer = (buffer.length / Disk.blockSize) + seek + 1;
        int count = 0; // Used to check if it is equivalent to blocksForBuffer
        for(int i = 0; i < blocksForBuffer && i < inode.direct.length; i++){
            if(inode.direct[i] == -1){
                inode.direct[i] = (short)superblock.getFreeBlock();
                count++;
            }
        }
        
        // Need to get some indirect blocks
        // if(count < blocksForBuffer){}

        // Find the write block to write to. int block
        int currentBlock = block;
        byte[] toWrite = new byte[Disk.blockSize];
        SysLib.rawread(inode.direct[block], toWrite);
        int retVal = 0;
        for(int i = 0; i < buffer.length; i++){
            buffer[i] = toWrite[seek % 512]; // Copy over byte content
            retVal++;
            if(seek == 511){ // Go to next block cause ran out of space in current block
                SysLib.rawwrite(inode.direct[block], toWrite); // save current
                block++; //move to next
                SysLib.rawread(inode.direct[block], toWrite); // Grab next block available
            }
        }

        // Write content to block        
        //SysLib.rawread(inode.direct[block], toWrite); // Get original content
        //System.arraycopy(buffer, 0, toWrite, seek % 512, buffer.length); // Copy to appropriate slot
            
            
        //SysLib.rawwrite(inode.direct[block], toWrite);
           
            //SysLib.rawread(inode.direct[i], toWrite);
            //SysLib.cout("toWrite again: \n");
            //for(int j = 0; j < toWrite.length; j++){
            //    SysLib.cout("toWrite["+j+"] : " + toWrite[j] + "\n");
            //}
                        
            //byte[] debug = new byte[Disk.blockSize];
            //SysLib.rawread(inode.direct[i], debug);
            //SysLib.cout("Debug After Reading " + inode.direct[i] + ": \n");
            //for(int j = 0; j < debug.length; j++){
            //    SysLib.cout("Direct["+j+"]: " + debug[j] + "\n");
            //}
           
        ftEnt.seekPtr += buffer.length;
        
        // Return number of bytes written
        return retVal;
    }

    // read------------------------
    // DOESN'T READ FROM INDIRECT BLOCKS
    public int read(FileTableEntry ftEnt, byte[] buffer) {
        SysLib.cout("read("+ftEnt+", " + buffer + ");\n");
        // Check for valid inputs
        if(buffer == null || ftEnt == null) {
            SysLib.cout("Invalid\n");
            return -1;
        }

        Inode inode = ftEnt.inode;
        int block = ftEnt.seekPtr / 512;     // Which logical block are we looking at
                
        int retVal = 0;     // How many bytes are read
        byte[] toRead = new byte[Disk.blockSize];   // Used to read entire block
        SysLib.rawread(inode.direct[block], toRead); 
        for(int i = 0; i < buffer.length; i++){     // Read byte at a time
            buffer[i] = toRead[ftEnt.seekPtr % 512];
            retVal++;                               // Increment byte as read
            if(ftEnt.seekPtr == 511){               // If end of block
                block++;                            // Go to next block available
                if(inode.direct[block] != -1) {         // only if available
                    SysLib.cout("rawreading another block\n");
                    SysLib.rawread(inode.direct[block], toRead);
                } else {
                    SysLib.cout("returning -1\n");
                    return -1;                      // Past file size
                }
            }
        }
       
        for(int i = 0; i < buffer.length; i++) {
            SysLib.cout("Buffer["+i+"]: " + buffer[i] + "\n");
        }

        // Return number of bytes read
        return retVal;
    }

    // seek----------------------------
    public int seek(FileTableEntry ftEnt, int offset, int whence) {
        // Change the seek pointer by the offset depending on whence
        switch(whence) {
            case SEEK_SET: 
                SysLib.cout("SEEK_SET\n");
                ftEnt.seekPtr = 0;
                ftEnt.seekPtr = offset;
                // Check to make sure we don't go over using fsize()
                if(ftEnt.seekPtr > fsize(ftEnt) && ftEnt.seekPtr < 0) {
                    ftEnt.seekPtr = fsize(ftEnt);
                }
                SysLib.cout("ftEnt.seekPtr = " + ftEnt.seekPtr + "\n");
                return ftEnt.seekPtr;
            
            case SEEK_CUR:
                ftEnt.seekPtr += offset;
                // Check to make sure we don't go over using fsize()
                if(ftEnt.seekPtr > fsize(ftEnt)){
                    ftEnt.seekPtr = fsize(ftEnt);
                }

                SysLib.cout("SEEK_CUR\n");
                SysLib.cout("ftEnt.seekPtr = " + ftEnt.seekPtr + "\n");

                return ftEnt.seekPtr;

            case SEEK_END:
                // Use fsize() to get the end of file
                // Check to make sure we don't go under using fsize()
                ftEnt.seekPtr = fsize(ftEnt) - offset;
                if(ftEnt.seekPtr < 0){
                    ftEnt.seekPtr = 0;
                }
                SysLib.cout("SEEK_END\n");
                SysLib.cout("ftEnt.seekPtr = " + ftEnt.seekPtr + "\n");
                break;

            default:
                SysLib.cout("Default\n");
                return -1;
        }

        return ftEnt.seekPtr;
    }

    // delete--------------------
    public boolean delete(String filename) {
        SysLib.cout("Delete " + filename + "\n");

        // get the file
        FileTableEntry entry = open(filename, "w");
        SysLib.cout("Entry: " + entry + "\n");

        // mark for delete, close, and delete
        if(filetable.ffree(entry)){
            SysLib.cout("free(entry)  == true \n");
            return true;
        } else {
            SysLib.cout("free(entry) == false\n");
            return false;
        }
    }

    public int fsize(FileTableEntry entry) {
        return entry.inode.length;
    }
}
