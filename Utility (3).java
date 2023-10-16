import com.sun.security.jgss.GSSUtil;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class Utility {

    public void Compress(int[][][] pixels, String outputFileName) throws IOException {
        Map<String, Integer> frequencyTable = new HashMap<>();
        String[][] hexaPixelsAry = new String[pixels.length][];

        for (int i = 0; i < pixels.length; i++) {
            String[] pixel1D = new String[pixels[i].length];

            for (int j = 0; j < pixels[i].length; j++) {
                int red = pixels[i][j][0];
                int green = pixels[i][j][1];
                int blue = pixels[i][j][2];
                String hexDeci = toHexDeci(red, green, blue);
                pixel1D[j] = hexDeci;
                if (frequencyTable.containsKey(hexDeci)) {
                    int frequency = frequencyTable.get(hexDeci);
                    frequency++;
                    frequencyTable.put(hexDeci, frequency);
                } else {
                    frequencyTable.put(hexDeci, 1);
                }
            }

            hexaPixelsAry[i] = pixel1D;
        }
        //Frequency Table to PriorityQueue
        PriorityQueue<HuffmanNode> nodes = generatePriorityQueue(frequencyTable);

        //Build Huffman Tree
        HuffmanNode huffmanTree = generateAHuffmanTree(nodes);
        System.out.println("Number of nodes in tree: " + countTotalNodes(huffmanTree));


        // Generate Huffman codes
        Map<String, String> huffmanCodes = new HashMap<>();
        generateHuffmanCodes(huffmanTree, "", huffmanCodes);

        // Write the Huffman-encoded data
        writeEncodedData(hexaPixelsAry, outputFileName, huffmanCodes);
    }


    public int[][][] Decompress(String inputFileName) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(inputFileName))) {
            // Read Huffman codes
            Map<String, String> huffmanCodes = (Map<String, String>) ois.readObject();

            // Read dimensions of the array
            int depth = ois.readInt();
            int height = ois.readInt();
//            int width = 3; // r, g, b

            int[][][] pixels = new int[depth][height][];

            // Read the Huffman-encoded data and decode it
            for (int i = 0; i < depth; i++) {
                for (int j = 0; j < height; j++) {
                    String code = ois.readUTF();
                    String s = longestPrefixMatching(code, huffmanCodes);
                    System.out.println(code);
                    int[] color = hexDeciToInt(s);

                }
            }

            return pixels;
        }
    }

    public String longestPrefixMatching(String code, Map<String, String> huffmanCodes) {
        for (int i = 1; i< code.length(); i++) {
            String temp = "";
            String newTemp = "";
            for (Map.Entry<String, String> entrySet : huffmanCodes.entrySet()) {
                if (entrySet.getValue().startsWith(code.substring(0, i))) {
                    newTemp = entrySet.getKey();
                    break;
                }
            }
            if (newTemp.equals(temp)) {
                return newTemp;
            }
        }

        return null;
    }

    public int[] hexDeciToInt(String hexaDeci) {
        String red = hexaDeci.substring(0, 2);
        String green = hexaDeci.substring(2, 4);
        String blue = hexaDeci.substring(4, 6);

        int redInt = Integer.parseInt(red, 16);
        int greenInt = Integer.parseInt(green, 16);
        int blueInt = Integer.parseInt(blue, 16);

        return new int[]{redInt, greenInt, blueInt};
    }

    //Function to take priority queue and convert into a huffman tree, returning the root node of the tree
    public HuffmanNode generateAHuffmanTree(PriorityQueue<HuffmanNode> priorityQueue) {
        // Build a Huffman tree
        while (priorityQueue.size() > 1) {
            HuffmanNode left = priorityQueue.remove();
            HuffmanNode right = priorityQueue.remove();
            HuffmanNode parent = new HuffmanNode(left, right);
            priorityQueue.add(parent);
        }
        return priorityQueue.peek();
    }

    public String toHexDeci(int red, int green, int blue) {
        return String.valueOf(Integer.toHexString(0x100 | red).charAt(1)) +
                Integer.toHexString(0x100 | green).charAt(1) +
                Integer.toHexString(0x100 | blue).charAt(1);
    }

    public int countTotalNodes(HuffmanNode root) {
        if (root.isLeaf())
            return 0;

        int l = countTotalNodes(root.left);
        int r = countTotalNodes(root.right);

        return 1 + l + r;
    }


    private PriorityQueue<HuffmanNode> generatePriorityQueue(Map<String, Integer> frequencyTable) {
        return frequencyTable.entrySet().stream()
                .map(entry -> new HuffmanNode(entry.getKey(), entry.getValue()))
                .sorted()
                .collect(Collectors.toCollection(PriorityQueue::new));
    }

    private void generateHuffmanCodes(HuffmanNode node, String code, Map<String, String> huffmanCodes) {
        if (node.isLeaf()) {
            huffmanCodes.put(node.value, code);
        } else {
            generateHuffmanCodes(node.left, code + "0", huffmanCodes);
            generateHuffmanCodes(node.right, code + "1", huffmanCodes);
        }
    }

    private void writeEncodedData(String[][] pixels, String outputFileName, Map<String, String> huffmanCodes) throws
            IOException {
        StringBuilder sb = new StringBuilder();
        Arrays.stream(pixels)
                .forEach(plane -> Arrays.stream(plane)
                        .forEach(value -> {
                            String huffmanCode = huffmanCodes.get(value);
                            sb.append(huffmanCode);

                        })
                );

        byte[] toWrite = new byte[sb.length() / 8];
        for (int i = 0; i < sb.length() / 8; i++) {
            toWrite[i] = (byte) Integer.parseInt(
                    sb.substring(i * 8, (i + 1) * 8), 16);
        }

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(outputFileName))) {
            // Write Huffman codes and dimensions of to the output file);
//            oos.writeObject(byteArray);
            oos.writeObject(huffmanCodes);
            oos.writeInt(pixels.length);
            oos.writeInt(pixels[0].length);
            oos.write(toWrite);
        }
    }

    public void traversePreOrder(StringBuilder sb, HuffmanNode node) {
        if (node != null) {
            sb.append(node.getValue());
            sb.append("\n");
            traversePreOrder(sb, node.getLeft());
            traversePreOrder(sb, node.getRight());
        }
    }

    private static class HuffmanNode implements Comparable<HuffmanNode> {
        String value;
        int frequency;
        HuffmanNode left;
        HuffmanNode right;

        public HuffmanNode(String value, int frequency) {
            this.value = value;
            this.frequency = frequency;
        }

        public HuffmanNode(HuffmanNode left, HuffmanNode right) {
            this.left = left;
            this.right = right;
            this.frequency = left.frequency + right.frequency;
        }

        public boolean isLeaf() {
            return left == null && right == null;
        }

        @Override
        public int compareTo(HuffmanNode other) {
            return Integer.compare(frequency, other.frequency);
        }

        public String getValue() {
            return value;
        }

        public HuffmanNode getLeft() {
            return left;
        }

        public HuffmanNode getRight() {
            return right;
        }
    }
}