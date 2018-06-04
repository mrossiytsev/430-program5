// MARK: - Class
public class Inode {
        // MARK: Properties
        private final static int iNodeSize = 32;
        private final static int directSize = 11;

        public int length;
        public short count;
        public short flag;
        public short direct[] = new short[directSize];
        public short indirect;

        // MARK: Life Cycle
        public Inode () {
                //SysLib.cout("INITIALIZED DEFAULT INODE\n");
                length = 0;
                count = 0;
                flag = 1;
                for (int i = 0; i < directSize; i++) {
                        direct[i] = -1;
                }
                indirect = -1;
        }

        public Inode(short iNumber) {

                //SysLib.cout("INITIALIZING INODE WITH iNumber: " + iNumber + "\n");
                // check to see if the passed Inumber is valid on disk
                        // if it isn't, don't do any initialization
                if (iNumber < 0) {
                        return;
                }

                // retrieve existing iNode from disk
                int blockNumber = 1 + iNumber / 16;
                byte[] data = new byte[Disk.blockSize];
                SysLib.rawread(blockNumber, data);

                // locate information from found iNode on disk and update local values
                int offset = (iNumber % 16) * 32;
                length = SysLib.bytes2int(data, offset);
                offset += 4;               

                count = SysLib.bytes2short(data, offset);
                offset += 2;

                flag = SysLib.bytes2short(data, offset);
                offset += 2;

                //SysLib.cout("Length: " + length + "....Count: " + count + "....Flag: " + flag + "\n");

                for (int i = 0; i < directSize; i++, offset += 2) {
                        direct[i] = SysLib.bytes2short(data, offset);
                }

                indirect = SysLib.bytes2short(data, offset);

                //for(int i = 0; i < directSize; i++){
                //    SysLib.cout("Direct["+i+"]: " + direct[i] + "\n");
                //}
                //SysLib.cout("Indirect: " + indirect + "\n");
                //SysLib.cout("INITIALIZED INODE WITH iNumber: " + iNumber + "\n");
        }


    	// MARK: Public
        public int toDisk (short iNumber) {

                //SysLib.cout("Inode with iNumber: " + iNumber + "\n");
                // update local values
                if (iNumber < 0) {
                        return -1;
                }

                int blockNumber = 1 + iNumber / 16;
                int offset = (iNumber % 16) * iNodeSize;
                byte[] data = new byte[Disk.blockSize];

                SysLib.int2bytes(length, data, offset);
                offset += 4;

                SysLib.short2bytes(count, data, offset);
                offset += 2;

                SysLib.short2bytes(flag, data, offset);
                offset += 2;

                // TODO: finish this, trav boy
                for (int i = 0; i < directSize; i++) {
                        SysLib.short2bytes(direct[i], data, offset);
                        offset += 2;
                }

                byte[] newData = new byte[512];
                SysLib.rawread(blockNumber, newData);

                offset = (iNumber % 16) * 32;

                System.arraycopy(data, 0, newData, offset, iNodeSize);
                SysLib.rawwrite(blockNumber, newData);

                return 0;
        }

        public short getIndexBlockNumber() {
                return 5;
        }

	    public boolean setIndexBlock(short indexBlockNumber) {
                for (int i = 0; i < directSize; i++) {
                        if (direct[i] == -1) {
                                return false;
                        }
                }

                if (indirect != -1) {
                        return false;
                }

                indirect = indexBlockNumber;
                byte[] data = new byte[512];

                for (int i = 0; i < (512/2); i++) {
                        SysLib.short2bytes((short) -1, data, i * 2);
                }

                SysLib.rawwrite(indexBlockNumber, data);

                return true;
        }

	    public short findTargetBlock(int offset) {
                int target = offset / 512;

                // if target is in the direct pointers
                if (target < directSize) {
                        return direct[target];
                }

                // otherwise check if indirect is in a bad state
                if (indirect < 0) {
                        return -1;
                }

                byte[] data = new byte[512];
                SysLib.rawread(indirect, data);

                int blockSpace = (target - directSize) * 2;
                return SysLib.bytes2short(data, blockSpace);
        }
}

