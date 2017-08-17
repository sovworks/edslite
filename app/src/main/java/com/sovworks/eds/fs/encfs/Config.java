package com.sovworks.eds.fs.encfs;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Base64;

import com.sovworks.eds.android.Logger;
import com.sovworks.eds.android.R;
import com.sovworks.eds.exceptions.ApplicationException;
import com.sovworks.eds.fs.File;
import com.sovworks.eds.fs.Path;
import com.sovworks.eds.fs.encfs.codecs.data.AESDataCodecInfo;
import com.sovworks.eds.fs.encfs.codecs.name.BlockNameCodecInfo;
import com.sovworks.eds.fs.util.PathUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class Config
{
    public static final String CONFIG_FILENAME2 = "encfs6.xml";
    public static final String CONFIG_FILENAME = '.' + CONFIG_FILENAME2;

    public static Path getConfigFilePath(com.sovworks.eds.fs.Directory dir) throws IOException
    {
        Path p = PathUtil.buildPath(dir.getPath(), CONFIG_FILENAME);
        if(p!=null && p.isFile())
            return p;
        p = PathUtil.buildPath(dir.getPath(), CONFIG_FILENAME2);
        return p != null && p.isFile() ? p : null;
    }

    public void read(Path pathToRootFolder) throws IOException, ApplicationException
    {
        Path p = getConfigFilePath(pathToRootFolder.getDirectory());
        if(p!=null)
            read(p.getFile());
        else
            throw new ApplicationException("EncFs config file doesn't exist");

    }

    public void read(File configFile) throws IOException, ApplicationException
    {
        InputStream inp = configFile.getInputStream();
        try
        {
            read(inp);
        }
        finally
        {
            inp.close();
        }
    }

    public void read(InputStream config) throws ApplicationException
    {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder;
        try
        {
            dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(config);
            //optional, but recommended
            //read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
            doc.getDocumentElement().normalize();
            NodeList nl = doc.getElementsByTagName("cfg");
            if(nl.getLength() == 0)
                throw new IllegalArgumentException("cfg element not found");
            Node n = nl.item(0);
            if(n.getNodeType() != Node.ELEMENT_NODE)
                throw new IllegalArgumentException("wrong document structure");
            readCfgElement((org.w3c.dom.Element) n);
        }
        catch (Exception e)
        {
            throw new ApplicationException("Failed reading the config file", e);
        }
    }

    public void write(Path pathToRootFolder) throws IOException, ApplicationException
    {
        write(PathUtil.getFile(pathToRootFolder, CONFIG_FILENAME));
    }

    public void write(File configFile) throws IOException, ApplicationException
    {
        OutputStream out = configFile.getOutputStream();
        try
        {
            write(out);
        }
        finally
        {
            out.close();
        }
    }

    public void write(OutputStream out) throws ApplicationException
    {
        try
        {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            Document doc = docBuilder.newDocument();
            doc.setXmlStandalone(true);
            Element el = doc.createElement("boost_serialization");
            doc.appendChild(el);
            el.setAttribute("signature", "serialization::archive");
            el.setAttribute("version", "14");
            el.appendChild(makeCfgElement(doc));

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "boost_serialization");
            DOMSource source = new DOMSource(doc);

            StreamResult result = new StreamResult(out);
            transformer.transform(source, result);
        }
        catch (Exception e)
        {
            throw new ApplicationException("Failed making EncFs config file", e);
        }
    }

    public void initNew(Context context)
    {
        _creator = makeCreatorString(context);
        _subVersion = 20100713;
        _kdfIterations = 100000;
        _desiredKDFDuration = 500;
        _keySizeBits = 192;
        _blockSize = 1024;
        _blockMACRandBytes = _blockMACBytes = 0;
        _uniqueIV = true;
        _chainedNameIV = true;
        _externalIVChaining = false;
        _allowHoles = true;
        _dataCipher = (DataCodecInfo) new AESDataCodecInfo().select(this);
        _nameCipher = (NameCodecInfo) new BlockNameCodecInfo().select(this);
    }

    public boolean useUniqueIV()
    {
        return _uniqueIV;
    }

    public void useUniqueIV(boolean val)
    {
        _uniqueIV = val;
    }

    public DataCodecInfo getDataCodecInfo()
    {
        return _dataCipher;
    }

    public void setDataCodecInfo(DataCodecInfo codecInfo)
    {
        _dataCipher = codecInfo;
    }

    public NameCodecInfo getNameCodecInfo() { return _nameCipher; }

    public void setNameCodecInfo(NameCodecInfo codecInfo)
    {
        _nameCipher = codecInfo;
    }

    public byte[] getSalt()
    {
        return _salt;
    }

    public void setSalt(byte[] salt) { _salt = salt; }

    public int getKDFIterations()
    {
        return _kdfIterations;
    }

    public void setKDFIterations(int val) { _kdfIterations = val; }

    public int getKeySize()
    {
        return _keySizeBits/8;
    }

    public void setKeySize(int numBytes)
    {
        _keySizeBits = numBytes * 8;
    }

    public int getBlockSize()
    {
        return _blockSize;
    }

    public void setBlockSize(int val) { _blockSize = val; }

    public byte[] getEncryptedVolumeKey()
    {
        return _keyData;
    }

    public void setEncryptedVolumeKey(byte[] val) { _keyData = val; }

    public boolean useChainedNameIV()
    {
        return _chainedNameIV;
    }

    public void useChainedNameIV(boolean val)
    {
        _chainedNameIV = val;
    }

    public boolean useExternalFileIV()
    {
        return _externalIVChaining;
    }

    public void useExternalFileIV(boolean val)
    {
        _externalIVChaining = val;
    }

    public void allowHoles(boolean val)
    {
        _allowHoles = val;
    }

    public boolean allowHoles()
    {
        return _allowHoles;
    }

    public int getMACBytes()
    {
        return _blockMACBytes;
    }

    public void setMACBytes(int val) { _blockMACBytes = val; }

    public int getMACRandBytes()
    {
        return _blockMACRandBytes;
    }

    public void setMACRandBytes(int val) { _blockMACRandBytes = val; }

    private String _creator;
    private int _subVersion;
    private DataCodecInfo _dataCipher;
    private NameCodecInfo _nameCipher;
    private int _keySizeBits;
    private int _blockSize;

    private byte[] _keyData;
    private byte[] _salt;

    private int _kdfIterations;
    private int _desiredKDFDuration;

    private int _blockMACBytes;      // MAC headers on blocks..
    private int _blockMACRandBytes;  // number of random bytes in the block header

    private boolean _uniqueIV;            // per-file Initialization Vector
    private boolean _externalIVChaining;  // IV seeding by filename IV chaining

    private boolean _chainedNameIV;  // filename IV chaining
    private boolean _allowHoles;     // allow holes in files (implicit zero blocks)

    private Iterable<NameCodecInfo> getSupportedNameCodecs()
    {
        return FS.getSupportedNameCodecs();
    }

    private Iterable<DataCodecInfo> getSupportedDataCodecs()
    {
        return FS.getSupportedDataCodecs();
    }

    private void readCfgElement(Element cfg)
    {
        _subVersion = getParam(cfg, "version", 20100713);
        _creator = getParam(cfg, "creator", "");
        _dataCipher = (DataCodecInfo) loadAlgInfo(cfg, "cipherAlg", getSupportedDataCodecs(), null);
        _nameCipher = (NameCodecInfo) loadAlgInfo(cfg, "nameAlg", getSupportedNameCodecs(), null);
        _keySizeBits = getParam(cfg, "keySize", 0);
        _blockSize = getParam(cfg, "blockSize", 0);
        _uniqueIV = getParam(cfg, "uniqueIV", true);
        _chainedNameIV = getParam(cfg, "chainedNameIV", true);
        _externalIVChaining = getParam(cfg, "externalIVChaining", false);
        _blockMACBytes = getParam(cfg, "blockMACBytes", 0);
        _blockMACRandBytes = getParam(cfg, "blockMACRandBytes", 0);
        _allowHoles = getParam(cfg, "allowHoles", true);

        _keyData = getBytes(cfg, "encodedKeyData");
        int size = getParam(cfg, "encodedKeySize", 0);
        if(size>0 && size!=_keyData.length)
            throw new IllegalArgumentException("Failed decoding key data");

        _salt = getBytes(cfg, "saltData");
        size = getParam(cfg, "saltLen", 0);
        if(size>0 && size!=_salt.length)
            throw new IllegalArgumentException("Failed decoding salt data");
        _kdfIterations = getParam(cfg, "kdfIterations", 0);
        _desiredKDFDuration = getParam(cfg, "desiredKDFDuration", 0);
    }

    private int getParam(Element cfg, String paramName, int defaultValue)
    {
        String s = getParam(cfg, paramName, null);
        if(s == null)
            return defaultValue;
        return Integer.valueOf(s);
    }

    private boolean getParam(Element cfg, String paramName, boolean defaultValue)
    {
        String s = getParam(cfg, paramName, null);
        if(s == null)
            return defaultValue;
        return !"0".equals(s);
    }

    private String getParam(Element cfg, String paramName, String defaultValue)
    {
        NodeList nl = cfg.getElementsByTagName(paramName);
        if(nl.getLength() == 0)
            return defaultValue;
        Node n = nl.item(0);
        String data = n.getTextContent();
        return data == null ? defaultValue : data;
    }

    private byte[] getBytes(Element cfg, String paramName)
    {
        String encoded = getParam(cfg, paramName, null);
        if(encoded == null)
            return null;
        return Base64.decode(encoded, Base64.DEFAULT);
    }

    private AlgInfo loadAlgInfo(Element cfg, String paramName, Iterable<? extends AlgInfo> supportedAlgs, AlgInfo defaultValue)
    {
        NodeList nl = cfg.getElementsByTagName(paramName);
        if(nl.getLength() == 0)
            return defaultValue;
        Node n = nl.item(0);
        if(n.getNodeType() != Node.ELEMENT_NODE)
            throw new IllegalArgumentException("Wrong document structure");
        String algName = getParam((Element)n, "name", null);
        if(algName == null)
            throw new IllegalArgumentException("Name is not specified for " + paramName);
        int major = getParam((Element)n, "major", 0);
        int minor = getParam((Element)n, "minor", 0);
        for(AlgInfo info: supportedAlgs)
        {
            if(algName.equals(info.getName()) && info.getVersion1() >= major && info.getVersion2() >= minor)
                return info.select(this);
        }
        throw new IllegalArgumentException("Unsupported algorithm: " + algName + " major=" + major + " minor=" + minor);
    }

    private Element makeCfgElement(Document doc)
    {
        Element cfgEl = doc.createElement("cfg");
        cfgEl.setAttribute("class_id", "0");
        cfgEl.setAttribute("tracking_level", "0");
        cfgEl.setAttribute("version", "20");

        Element el = doc.createElement("version");
        cfgEl.appendChild(el);
        el.setTextContent(String.valueOf(_subVersion));

        el = doc.createElement("creator");
        cfgEl.appendChild(el);
        el.setTextContent(_creator);

        el = makeAlgInfoElement(doc, "cipherAlg", _dataCipher);
        cfgEl.appendChild(el);
        el.setAttribute("class_id", "1");
        el.setAttribute("tracking_level", "0");
        el.setAttribute("version", "0");

        el = makeAlgInfoElement(doc, "nameAlg", _nameCipher);
        cfgEl.appendChild(el);

        el = doc.createElement("keySize");
        cfgEl.appendChild(el);
        el.setTextContent(String.valueOf(_keySizeBits));

        el = doc.createElement("blockSize");
        cfgEl.appendChild(el);
        el.setTextContent(String.valueOf(_blockSize));

        el = doc.createElement("uniqueIV");
        cfgEl.appendChild(el);
        el.setTextContent(_uniqueIV ? "1" : "0");

        el = doc.createElement("chainedNameIV");
        cfgEl.appendChild(el);
        el.setTextContent(_chainedNameIV ? "1" : "0");

        el = doc.createElement("externalIVChaining");
        cfgEl.appendChild(el);
        el.setTextContent(_externalIVChaining ? "1" : "0");

        el = doc.createElement("blockMACBytes");
        cfgEl.appendChild(el);
        el.setTextContent(String.valueOf(_blockMACBytes));

        el = doc.createElement("blockMACRandBytes");
        cfgEl.appendChild(el);
        el.setTextContent(String.valueOf(_blockMACRandBytes));

        el = doc.createElement("allowHoles");
        cfgEl.appendChild(el);
        el.setTextContent(_allowHoles ? "1" : "0");

        el = doc.createElement("encodedKeySize");
        cfgEl.appendChild(el);
        el.setTextContent(String.valueOf(_keyData.length));

        el = doc.createElement("encodedKeyData");
        cfgEl.appendChild(el);
        el.setTextContent(Base64.encodeToString(_keyData, Base64.DEFAULT));

        el = doc.createElement("saltLen");
        cfgEl.appendChild(el);
        el.setTextContent(String.valueOf(_salt.length));

        el = doc.createElement("saltData");
        cfgEl.appendChild(el);
        el.setTextContent(Base64.encodeToString(_salt, Base64.DEFAULT));

        el = doc.createElement("kdfIterations");
        cfgEl.appendChild(el);
        el.setTextContent(String.valueOf(_kdfIterations));

        el = doc.createElement("desiredKDFDuration");
        cfgEl.appendChild(el);
        el.setTextContent(String.valueOf(_desiredKDFDuration));

        return cfgEl;
    }

    private Element makeAlgInfoElement(Document doc, String paramName, AlgInfo info)
    {
        Element el = doc.createElement(paramName);

        Element el2 = doc.createElement("name");
        el.appendChild(el2);
        el2.setTextContent(info.getName());

        el2 = doc.createElement("major");
        el.appendChild(el2);
        el2.setTextContent(String.valueOf(info.getVersion1()));

        el2 = doc.createElement("minor");
        el.appendChild(el2);
        el2.setTextContent(String.valueOf(info.getVersion2()));

        return el;
    }

    private String makeCreatorString(Context context)
    {
        if(context == null)
            return "EDS";
        String verName = "";
        try
        {
            verName = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        }
        catch (PackageManager.NameNotFoundException e)
        {
            Logger.showAndLog(context, e);
        }
        return String.format(
                "%s v%s",
                context.getString(R.string.eds),
                verName
        );
    }
}
