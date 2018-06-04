public class SuperBlock {
    public int totalBlocks;
    public int totalInodes;
    public int freeList;
    //public int freeListTail;

    public int inodeBlocks;

    private final int DEFAULT_NUM_OF_INODES = 64;
    private final int INODES_PER_BLOCK = 16;

    // Constructor
    public SuperBlock(int disksize){
        // Read currently saved SuperBlock data
        byte[] superblock = new byte[Disk.blockSize];
        SysLib.rawread(0, superblock);
        totalBlocks = SysLib.bytes2int(superblock, 0);
        totalInodes = SysLib.bytes2int(superblock, 4);
        inodeBlocks = (totalInodes / INODES_PER_BLOCK);
        freeList = SysLib.bytes2int(superblock, 8);
        //freeListTail = SysLib.bytes2int(superblock,12);

        // If the data is incosistent, reformat with default
        if(totalBlocks != disksize || totalInodes < 0 || freeList < 2){
            totalBlocks = disksize;
            format(DEFAULT_NUM_OF_INODES);
        }
       
    }

    // format
    public void format(int numOfInodes){
        //SysLib.cout("\nformat( " + numOfInodes + " )\n");
        
        if(numOfInodes < 0) numOfInodes = DEFAULT_NUM_OF_INODES;

        // Set the super block with the right information
        totalInodes = numOfInodes;
        
        // Input all inodes 
        for(int i = 0; i < totalInodes; i++){
            Inode inode = new Inode((short)i);
        }

        // Find out what block we're at after inodes 
        inodeBlocks = (totalInodes / INODES_PER_BLOCK);
        freeList = inodeBlocks + 1;
        
        // Save, to disk, all free list indices
        byte[] temp = new byte[Disk.blockSize];
        int i;
        for(i = freeList; i < totalBlocks - 1; i++){
            SysLib.int2bytes(i+1, temp, 0); // This was changed from i + 1 to 1
            SysLib.rawwrite(i, temp);
        }

        // Insert the final freeList index (-1) to indicate end of list
        SysLib.int2bytes(-1, temp, 0);
        //freeListTail = i;
        SysLib.rawwrite(i, temp);
    }

    // sync
    public boolean sync(){
        //SysLib.cout("\nSync()\n");
        byte[] temp = new byte[Disk.blockSize];

        // Save all our data
        SysLib.int2bytes(totalBlocks, temp, 0);
        SysLib.int2bytes(totalInodes, temp, 4);
        SysLib.int2bytes(freeList, temp, 8);
        
        //SysLib.cout("sync() and temp ["+temp+"] has a size of " + temp.length + "\n");
        if(SysLib.rawwrite(0, temp) == -1) return false;
        else return true;
    }

    // getFreeBlock - Dequeue the top of the free list 
    public int getFreeBlock(){
        //SysLib.cout("\ngetFreeBlock()\n");

        if(freeList != -1){
            // Get the info associated with the current free list block
            byte[] temp = new byte[Disk.blockSize];
            SysLib.rawread(freeList, temp);

            //SysLib.cout("getFreeBlock(): " + temp + " has a size " + temp.length + "\n");

            // Get the actual integer for next free block
            //int newHead = SysLib.bytes2int(temp, 0);
            int currHead = freeList;
            freeList = SysLib.bytes2int(temp,0);

            byte[] emptyByte = new byte[Disk.blockSize];
            for(int i = 0; i < emptyByte.length; i++){
                emptyByte[i] = (byte)0;
            }
            SysLib.rawwrite(currHead, emptyByte);

            //SysLib.cout("new freeList: " + freeList + ".....old freeList: " +currHead+ "\n");
            
            //Debugging
            //byte[] debug = new byte[Disk.blockSize];
            //SysLib.rawread(17, debug);
            //int d2 = SysLib.bytes2int(debug, 0);
            //SysLib.cout("17 -> " + d2 + "\n");
                //Contents of 17
             //int d3 = SysLib.bytes2int(debug, 4);
            //SysLib.cout("17 -> " + d3 + "\n");


            return currHead;
        } else {
            return -1;
        }
    }

    // returnBlock - enqueue a given block to the end of the free list    
    public boolean returnBlock(int blockNumber){
        //SysLib.cout("\nreturnBlock("+blockNumber+")\n");
        // Placing onto head of list instead
        if(blockNumber > 0 && blockNumber < totalBlocks){
            // Get the free list in byte[] form
            byte[] temp = new byte[Disk.blockSize];
            //SysLib.rawread(freeListTail, temp);
            SysLib.int2bytes(freeList, temp, 0);
            SysLib.rawwrite(blockNumber, temp);

            // Write blockNumber to the end of the list
            //SysLib.int2bytes(blockNumber, temp, 0);

            //SysLib.rawwrite(freeListTail, temp);
            //SysLib.int2bytes(-1, temp, 0);
            //SysLib.rawwrite(blockNumber, temp);
            //SysLib.cout("returnBlock(): " + temp + " has a size " + temp.length + "\n");

            // Re-assign freeList
            freeList = blockNumber;
            //printFreeList(totalBlocks);
            return true;
        } else {
            return false;
        }
    }

    // Helper
    public void printFreeList(int index){
//        int currentBlock = (totalBlocks / INODES_PER_BLOCK) + 1;
//        byte[] temp = new byte[Disk.blockSize];

        for(int i = freeList; i < totalBlocks && i < index; i++){
            byte[] temp = new byte[Disk.blockSize];
            SysLib.rawread(i, temp);
            int currFree = SysLib.bytes2int(temp, 0);
            SysLib.cout("Block [" + i + "] has freeList pointer  [" + currFree + "] size: " + temp.length + "\n");
        }
    }

}
