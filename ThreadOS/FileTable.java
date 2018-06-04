import java.util.*;
/* FileTable
 * Author: Michael Rossiytsev
 * Date: 6/1/18
 * Description:
 *
 *
 *
 *
*/
public class FileTable {
    // Date Members
    private Vector table;   // Will hold FileTableEntries
    private Directory dir;  // Used to communicate existing/allocated files

    // Constructor: Instantiates table and directory
    // Preconditions:
    // Postconditions:
    public FileTable(Directory directory){
        this.table = new Vector();
        this.dir = directory;
    }

    // falloc:
    // Preconditions:
    // Postconditions: Updates the appropriate inode
    //                 
    public synchronized FileTableEntry falloc(String filename, String mode){
        //SysLib.cout("FileTable...falloc(" + filename  + ", " + mode + ")\n");
        //SysLib.cout("CURRENT FILETABLE:\n");
        //printData();

        short iNum = -1;
        Inode inode = null;
        
        iNum = dir.namei(filename);
        if(iNum == -1) // filename doesn't exist
        {
            iNum = dir.ialloc(filename);

            if(mode.equals("r")){                
                // Update inode
                inode = new Inode(iNum);
                inode.count++;
                inode.flag = 1; // in use
                inode.toDisk(iNum);

                // Insert FileTableEntry into table
                FileTableEntry FTE = new FileTableEntry(inode, iNum, mode);
                table.addElement(FTE);

                return null;
            } //else {
            //    iNum = dir.ialloc(filename);           
            //}
        } 
        else // filename exists
        {
            //SysLib.cout(filename + " exists\n");

            // Find the filetable entry that exists

            int i = 0;
            if(!table.isEmpty()){
            FileTableEntry currFTE = (FileTableEntry)table.get(i);
            for(; i < table.size() && currFTE.iNumber != iNum && !(currFTE.mode.equals(mode)); i++){
                currFTE = (FileTableEntry)table.get(i);
            }

            if(i >= table.size()) {            
                currFTE = null; // FTE with mode and filename doesn't exist
            }
                        
            if(mode.equals("r") && currFTE == null){ // if file exists but not in read
                return null; //??
//            } else if(mode.equals("r") && currFTE != null){
//                inode = new Inode(iNum);
//                inode.count++;
//                inode.toDisk(iNum);
//                return currFTE;
//            } else if (currFTE == null) {  //if file exists but not in "w" "w+" or "a"
//                // Create the file
            } else if (currFTE != null) {                
                inode = new Inode(iNum);
                inode.count++;
                inode.toDisk(iNum);
                return currFTE;
            }
            }
        }

        // Update inode
        inode = new Inode(iNum);
        inode.count++;
        inode.flag = 1; // in use
        inode.toDisk(iNum);

        // Insert FileTableEntry into table
        FileTableEntry FTE = new FileTableEntry(inode, iNum, mode);
        table.addElement(FTE);

        return FTE;
        
    }

    // ffree:
    // Preconditions:
    // Postconditions:
    public synchronized boolean ffree(FileTableEntry e){
        //SysLib.cout("FileTable....ffree\n");

        //SysLib.cout("CONTENTS OF TABLE:\n");
        //printData();
        
        // If file is currently open, don't delete
        e.inode.count--;
        e.inode.flag = 0; // unused
        e.inode.toDisk(e.iNumber);
        return table.remove(e);
    }


    // fempty: Uses vectory method to return empty or not.
    // Preconditions:
    // Postconditions:
    public synchronized boolean fempty() {
        return table.isEmpty();
    }


    //====================HELPER====================
    public void printData(){        
        for(int i = 0; i < table.size(); i++){
            FileTableEntry fte = (FileTableEntry)table.get(i);
            SysLib.cout("File:  " + fte + "\n");
            SysLib.cout("   iNumber:  " + fte.iNumber + "\n");
            SysLib.cout("   count:  " + fte.count + "\n");
            SysLib.cout("   mode:  " + fte.mode + "\n");
            SysLib.cout("   seekPtr:  " + fte.seekPtr + "\n");
        }
   }
}
