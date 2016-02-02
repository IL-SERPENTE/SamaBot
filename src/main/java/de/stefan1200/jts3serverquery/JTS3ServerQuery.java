/*
 * Decompiled with CFR 0_110.
 */
package de.stefan1200.jts3serverquery;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class JTS3ServerQuery {
    public boolean DEBUG = false;
    public String DEBUG_COMMLOG_PATH = "JTS3ServerQuery-communication.log";
    public String DEBUG_ERRLOG_PATH = "JTS3ServerQuery-error.log";
    public static final int LISTMODE_CLIENTLIST = 1;
    public static final int LISTMODE_CHANNELLIST = 2;
    public static final int LISTMODE_SERVERLIST = 3;
    public static final int LISTMODE_SERVERGROUPLIST = 4;
    public static final int LISTMODE_CLIENTDBLIST = 5;
    public static final int LISTMODE_PERMISSIONLIST = 6;
    public static final int LISTMODE_BANLIST = 7;
    public static final int LISTMODE_COMPLAINLIST = 8;
    public static final int LISTMODE_SERVERGROUPCLIENTLIST = 9;
    public static final int INFOMODE_SERVERINFO = 11;
    public static final int INFOMODE_CHANNELINFO = 12;
    public static final int INFOMODE_CLIENTINFO = 13;
    public static final int INFOMODE_CLIENTDBINFO = 14;
    public static final int PERMLISTMODE_CHANNEL = 21;
    public static final int PERMLISTMODE_SERVERGROUP = 22;
    public static final int PERMLISTMODE_CLIENT = 23;
    public static final int TEXTMESSAGE_TARGET_CLIENT = 1;
    public static final int TEXTMESSAGE_TARGET_CHANNEL = 2;
    public static final int TEXTMESSAGE_TARGET_VIRTUALSERVER = 3;
    public static final int TEXTMESSAGE_TARGET_GLOBAL = 4;
    public static final int EVENT_MODE_TEXTSERVER = 1;
    public static final int EVENT_MODE_TEXTCHANNEL = 2;
    public static final int EVENT_MODE_TEXTPRIVATE = 3;
    public static final int EVENT_MODE_SERVER = 4;
    public static final int EVENT_MODE_CHANNEL = 5;
    private boolean eventNotifyCheckActive = false;
    private TeamspeakActionListener actionClass = null;
    private int queryCurrentServerPort = -1;
    private int queryCurrentServerID = -1;
    private int queryCurrentClientID = -1;
    private int queryCurrentChannelID = -1;
    private String queryCurrentChannelPassword = null;
    private String queryCurrentClientName = null;
    private Socket socketQuery = null;
    private BufferedReader in = null;
    private PrintStream out = null;
    private PrintStream commLogOut = null;
    private PrintStream errLogOut = null;
    private Timer eventNotifyTimer = null;
    private TimerTask eventNotifyTimerTask = null;
    private String threadName = null;
    private SimpleDateFormat sdfDebug = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public JTS3ServerQuery() {
        this.threadName = "";
    }

    public JTS3ServerQuery(String threadName) {
        this.threadName = String.valueOf(threadName) + "_";
    }

    private synchronized void writeCommLog(String commMessage) {
        if (!this.DEBUG) {
            return;
        }
        if (this.DEBUG_COMMLOG_PATH == null) {
            return;
        }
        if (commMessage == null) {
            return;
        }
        try {
            if (this.commLogOut == null) {
                this.commLogOut = new PrintStream(this.DEBUG_COMMLOG_PATH, "UTF-8");
            }
            this.commLogOut.println(commMessage);
            this.commLogOut.flush();
        }
        catch (Exception e) {
            this.writeErrLog(e);
        }
    }

    private void writeErrLog(Exception e) {
        block6 : {
            if (!this.DEBUG) {
                return;
            }
            if (this.DEBUG_ERRLOG_PATH == null) {
                return;
            }
            if (e == null) {
                return;
            }
            try {
                if (this.errLogOut == null) {
                    this.errLogOut = new PrintStream(this.DEBUG_ERRLOG_PATH, "UTF-8");
                }
                this.errLogOut.println(this.sdfDebug.format(new Date(System.currentTimeMillis())));
                e.printStackTrace(this.errLogOut);
                this.errLogOut.flush();
            }
            catch (Exception ex) {
                if (!this.DEBUG) break block6;
                ex.printStackTrace();
            }
        }
    }

    private void eventNotifyRun() {
        if (this.eventNotifyCheckActive && this.isConnected()) {
            try {
                String inputLine;
                if (this.in.ready() && (inputLine = this.in.readLine()).length() > 0) {
                    this.writeCommLog("< " + inputLine);
                    this.handleAction(inputLine);
                }
            }
            catch (Exception ex) {
                this.writeErrLog(ex);
            }
        }
    }

    public void changeThreadName(String threadName) {
        this.threadName = String.valueOf(threadName) + "_";
    }

    public void setTeamspeakActionListener(TeamspeakActionListener listenerClass) {
        this.actionClass = listenerClass;
    }

    public void removeTeamspeakActionListener() throws TS3ServerQueryException {
        if (this.eventNotifyTimerTask != null) {
            this.removeAllEvents();
        }
        this.actionClass = null;
    }

    public void addEventNotify(int eventMode, int channelID) throws TS3ServerQueryException {
        if (this.actionClass == null) {
            throw new IllegalStateException("Use setTeamspeakActionListener() first!");
        }
        if (!this.isConnected()) {
            throw new IllegalStateException("Not connected to TS3 server!");
        }
        String command = null;
        if (eventMode == 4) {
            command = "servernotifyregister event=server";
        }
        if (eventMode == 5) {
            command = "servernotifyregister id=" + Integer.toString(channelID) + " event=channel";
        }
        if (eventMode == 1) {
            command = "servernotifyregister event=textserver";
        }
        if (eventMode == 2) {
            command = "servernotifyregister event=textchannel";
        }
        if (eventMode == 3) {
            command = "servernotifyregister event=textprivate";
        }
        if (command == null) {
            throw new IllegalArgumentException("Invalid eventMode given!");
        }
        HashMap<String, String> hmIn = this.doInternalCommand(command);
        if (!hmIn.get("id").equals("0")) {
            throw new TS3ServerQueryException("addEventNotify()", hmIn.get("id"), hmIn.get("msg"), hmIn.get("extra_msg"), hmIn.get("failed_permid"));
        }
        if (this.eventNotifyTimerTask == null) {
            this.eventNotifyTimerTask = new TimerTask(){

                public void run() {
                    JTS3ServerQuery.this.eventNotifyRun();
                }
            };
            this.eventNotifyTimer.schedule(this.eventNotifyTimerTask, 200, 200);
        }
    }

    public void removeAllEvents() throws TS3ServerQueryException {
        if (!this.isConnected()) {
            throw new IllegalStateException("Not connected to TS3 server!");
        }
        String command = "servernotifyunregister";
        HashMap<String, String> hmIn = this.doInternalCommand(command);
        if (!hmIn.get("id").equals("0")) {
            throw new TS3ServerQueryException("removeAllEvents()", hmIn.get("id"), hmIn.get("msg"), hmIn.get("extra_msg"), hmIn.get("failed_permid"));
        }
        if (this.eventNotifyTimerTask != null) {
            this.eventNotifyTimerTask.cancel();
            this.eventNotifyTimerTask = null;
        }
    }

    public void connectTS3Query(String ip, int queryport) throws Exception {
        this.connectTS3Query(ip, queryport, null, -1);
    }

    public void connectTS3Query(String ip, int queryport, String localIP, int localPort) throws Exception {
        if (this.socketQuery != null) {
            throw new IllegalStateException("Close currently open connection first!");
        }
        try {
            this.socketQuery = localIP != null && localPort >= 1 && localPort <= 65535 ? new Socket(ip, queryport, InetAddress.getByName(localIP), localPort) : new Socket(ip, queryport);
        }
        catch (Exception e) {
            this.socketQuery = null;
            throw e;
        }
        if (this.socketQuery.isConnected()) {
            try {
                this.in = new BufferedReader(new InputStreamReader(this.socketQuery.getInputStream(), "UTF-8"));
                this.out = new PrintStream(this.socketQuery.getOutputStream(), true, "UTF-8");
                this.socketQuery.setSoTimeout(5000);
                String serverIdent = this.in.readLine();
                this.writeCommLog("< " + serverIdent);
                if (!serverIdent.equals("TS3")) {
                    this.closeTS3Connection();
                    throw new IllegalStateException("Server does not respond as TS3 server!");
                }
                this.socketQuery.setSoTimeout(500);
                try {
                    String tmp = null;
                    do {
                        if ((tmp = this.in.readLine()) == null) {
                            throw new EOFException("Connection was closed by TS3 server, maybe banned?");
                        }
                        this.writeCommLog("< " + tmp);
                    } while (true);
                }
                catch (EOFException eof2) {
                    this.closeTS3Connection();
                    throw eof2;
                }
                catch (Exception eof2) {
                    this.socketQuery.setSoTimeout(40000);
                }
            }
            catch (Exception e) {
                this.closeTS3Connection();
                throw e;
            }
        }
        try {
            this.socketQuery.close();
        }
        catch (Exception e) {
            // empty catch block
        }
        this.socketQuery = null;
        if (this.eventNotifyTimer != null) {
            this.eventNotifyTimer.cancel();
            this.eventNotifyTimer = null;
        }
        if (this.eventNotifyTimerTask != null) {
            this.eventNotifyTimerTask.cancel();
            this.eventNotifyTimerTask = null;
        }
        this.eventNotifyTimer = new Timer(true);
        throw new IllegalStateException("Unknown connection error occurred!");
    }

    public void loginTS3(String loginname, String password) throws TS3ServerQueryException {
        if (!this.isConnected()) {
            throw new IllegalStateException("Not connected to TS3 server!");
        }
        HashMap<String, String> hmIn = this.doInternalCommand("login " + this.encodeTS3String(loginname) + " " + this.encodeTS3String(password));
        if (!hmIn.get("id").equals("0")) {
            throw new TS3ServerQueryException("loginTS3()", hmIn.get("id"), hmIn.get("msg"), hmIn.get("extra_msg"), hmIn.get("failed_permid"));
        }
        this.updateClientIDChannelID();
    }

    public void setDisplayName(String displayName) throws TS3ServerQueryException {
        if (!this.isConnected()) {
            throw new IllegalStateException("Not connected to TS3 server!");
        }
        if (displayName == null || displayName.length() < 3) {
            throw new IllegalArgumentException("displayName null or shorter than 3 characters!");
        }
        HashMap<String, String> hmIn = this.doInternalCommand("clientupdate client_nickname=" + this.encodeTS3String(displayName));
        if (!hmIn.get("id").equals("0")) {
            throw new TS3ServerQueryException("setDisplayName()", hmIn.get("id"), hmIn.get("msg"), hmIn.get("extra_msg"), hmIn.get("failed_permid"));
        }
        this.queryCurrentClientName = displayName;
    }

    public void selectVirtualServer(int serverID) throws TS3ServerQueryException {
        this.selectVirtualServer(serverID, false, false);
    }

    public void selectVirtualServer(int server, boolean selectPort) throws TS3ServerQueryException {
        this.selectVirtualServer(server, selectPort, false);
    }

    public void selectVirtualServer(int server, boolean selectPort, boolean virtual) throws TS3ServerQueryException {
        HashMap<String, String> hmIn;
        if (!this.isConnected()) {
            throw new IllegalStateException("Not connected to TS3 server!");
        }
        String command = selectPort ? "use port=" + Integer.toString(server) : "use sid=" + Integer.toString(server);
        if (virtual) {
            command = String.valueOf(command) + " -virtual";
        }
        if (!(hmIn = this.doInternalCommand(command)).get("id").equals("0")) {
            throw new TS3ServerQueryException("selectVirtualServer()", hmIn.get("id"), hmIn.get("msg"), hmIn.get("extra_msg"), hmIn.get("failed_permid"));
        }
        this.updateClientIDChannelID();
    }

    private void updateClientIDChannelID() throws TS3ServerQueryException {
        HashMap<String, String> hmIn = this.doInternalCommand("whoami");
        if (!hmIn.get("id").equals("0")) {
            throw new TS3ServerQueryException("updateClientIDChannelID()", hmIn.get("id"), hmIn.get("msg"), hmIn.get("extra_msg"), hmIn.get("failed_permid"));
        }
        if (hmIn.get("response") == null) {
            throw new IllegalStateException("No valid server response found!");
        }
        HashMap<String, String> response = this.parseLine(hmIn.get("response"));
        this.queryCurrentServerPort = Integer.parseInt(response.get("virtualserver_port"));
        this.queryCurrentServerID = Integer.parseInt(response.get("virtualserver_id"));
        this.queryCurrentClientID = Integer.parseInt(response.get("client_id"));
        this.queryCurrentClientName = response.get("client_nickname");
        this.queryCurrentChannelID = Integer.parseInt(response.get("client_channel_id"));
        this.queryCurrentChannelPassword = null;
    }

    public void closeTS3Connection() {
        if (this.eventNotifyTimerTask != null) {
            this.eventNotifyTimerTask.cancel();
            this.eventNotifyTimerTask = null;
            this.eventNotifyTimer.cancel();
            this.eventNotifyTimer = null;
        }
        this.queryCurrentClientID = -1;
        this.queryCurrentServerID = -1;
        this.queryCurrentChannelPassword = null;
        try {
            if (this.out != null) {
                this.out.println("quit");
                this.out.close();
                this.out = null;
                this.writeCommLog("> quit");
            }
        }
        catch (Exception e) {
            this.writeErrLog(e);
        }
        if (this.commLogOut != null) {
            this.commLogOut.close();
            this.commLogOut = null;
        }
        try {
            if (this.in != null) {
                this.in.close();
                this.in = null;
            }
        }
        catch (Exception e) {
            this.writeErrLog(e);
        }
        try {
            if (this.socketQuery != null) {
                this.socketQuery.close();
                this.socketQuery = null;
            }
        }
        catch (Exception e) {
            this.writeErrLog(e);
        }
    }

    public void deleteChannel(int channelID, boolean forceDelete) throws TS3ServerQueryException {
        if (!this.isConnected()) {
            throw new IllegalStateException("Not connected to TS3 server!");
        }
        String command = "channeldelete cid=" + Integer.toString(channelID) + " force=" + (forceDelete ? "1" : "0");
        HashMap<String, String> hmIn = this.doInternalCommand(command);
        if (!hmIn.get("id").equals("0")) {
            throw new TS3ServerQueryException("deleteChannel()", hmIn.get("id"), hmIn.get("msg"), hmIn.get("extra_msg"), hmIn.get("failed_permid"));
        }
        if (this.queryCurrentChannelID == channelID) {
            this.updateClientIDChannelID();
        }
    }

    public void moveClient(int clientID, int channelID, String channelPassword) throws TS3ServerQueryException {
        HashMap<String, String> hmIn;
        if (!this.isConnected()) {
            throw new IllegalStateException("Not connected to TS3 server!");
        }
        String command = "clientmove clid=" + Integer.toString(clientID) + " cid=" + Integer.toString(channelID);
        if (channelPassword != null && channelPassword.length() > 0) {
            command = String.valueOf(command) + " cpw=" + this.encodeTS3String(channelPassword);
        }
        if (!(hmIn = this.doInternalCommand(command)).get("id").equals("0")) {
            throw new TS3ServerQueryException("moveClient()", hmIn.get("id"), hmIn.get("msg"), hmIn.get("extra_msg"), hmIn.get("failed_permid"));
        }
        if (clientID == this.queryCurrentClientID) {
            this.queryCurrentChannelID = channelID;
            this.queryCurrentChannelPassword = channelPassword;
        }
    }

    public void kickClient(int cientID, boolean onlyChannelKick, String kickReason) throws TS3ServerQueryException {
        HashMap<String, String> hmIn;
        if (!this.isConnected()) {
            throw new IllegalStateException("Not connected to TS3 server!");
        }
        String command = "clientkick reasonid=" + (onlyChannelKick ? "4" : "5");
        if (kickReason != null && kickReason.length() > 0) {
            command = String.valueOf(command) + " reasonmsg=" + this.encodeTS3String(kickReason);
        }
        if (!(hmIn = this.doInternalCommand(command = String.valueOf(command) + " clid=" + Integer.toString(cientID))).get("id").equals("0")) {
            throw new TS3ServerQueryException("kickClient()", hmIn.get("id"), hmIn.get("msg"), hmIn.get("extra_msg"), hmIn.get("failed_permid"));
        }
    }

    public int getCurrentQueryClientID() {
        return this.queryCurrentClientID;
    }

    public int getCurrentQueryClientServerID() {
        return this.queryCurrentServerID;
    }

    public int getCurrentQueryClientServerPort() {
        return this.queryCurrentServerPort;
    }

    public int getCurrentQueryClientChannelID() {
        return this.queryCurrentChannelID;
    }

    public String getCurrentQueryClientName() {
        return this.queryCurrentClientName;
    }

    public void sendTextMessage(int targetID, int targetMode, String msg) throws TS3ServerQueryException {
        this.sendTextMessage(targetID, targetMode, msg, null);
    }

    public void sendTextMessage(int targetID, int targetMode, String msg, String channelPassword) throws TS3ServerQueryException {
        if (!this.isConnected()) {
            throw new IllegalStateException("Not connected to TS3 server!");
        }
        if (msg == null || msg.length() == 0) {
            throw new IllegalArgumentException("No message given!");
        }
        if (targetMode < 1 || targetMode > 4) {
            throw new IllegalArgumentException("Invalid targetMode given!");
        }
        HashMap<String, String> hmIn = null;
        String command = null;
        if (targetMode == 4) {
            command = "gm msg=" + this.encodeTS3String(msg);
            hmIn = this.doInternalCommand(command);
        } else if (targetMode == 2) {
            int oldChannel = -1;
            String oldChannelPassword = null;
            if (targetID != this.queryCurrentChannelID) {
                oldChannel = this.queryCurrentChannelID;
                oldChannelPassword = this.queryCurrentChannelPassword;
                this.moveClient(this.queryCurrentClientID, targetID, channelPassword);
            }
            command = "sendtextmessage targetmode=" + Integer.toString(targetMode) + " msg=" + this.encodeTS3String(msg);
            hmIn = this.doInternalCommand(command);
            if (oldChannel != -1) {
                this.moveClient(this.queryCurrentClientID, oldChannel, oldChannelPassword);
            }
        } else if (targetMode == 1) {
            command = "sendtextmessage targetmode=" + Integer.toString(targetMode) + " msg=" + this.encodeTS3String(msg) + " target=" + Integer.toString(targetID);
            hmIn = this.doInternalCommand(command);
        } else if (targetMode == 3) {
            int oldServer = -1;
            if (targetID != this.queryCurrentServerID) {
                oldServer = this.queryCurrentServerID;
                this.selectVirtualServer(targetID);
            }
            command = "sendtextmessage targetmode=" + Integer.toString(targetMode) + " msg=" + this.encodeTS3String(msg);
            hmIn = this.doInternalCommand(command);
            if (oldServer != -1) {
                this.selectVirtualServer(oldServer);
            }
        }
        if (!((String)hmIn.get("id")).equals("0")) {
            throw new TS3ServerQueryException("sendTextMessage()", hmIn.get("id"), hmIn.get("msg"), hmIn.get("extra_msg"), hmIn.get("failed_permid"));
        }
    }

    public HashMap<String, String> doCommand(String command) {
        if (command.startsWith("use ") || command.startsWith("clientmove ") || command.startsWith("channeldelete ")) {
            throw new IllegalArgumentException("This commands are not allowed here. Please use deleteChannel(), moveClient() or selectVirtualServer()!");
        }
        return this.doInternalCommand(command);
    }

    private synchronized HashMap<String, String> doInternalCommand(String command) {
        if (!this.isConnected()) {
            throw new IllegalStateException("Not connected to TS3 server!");
        }
        if (command == null || command.length() == 0) {
            throw new IllegalArgumentException("No command given!");
        }
        this.eventNotifyCheckActive = false;
        this.writeCommLog("> " + command);
        this.out.println(command);
        return this.readIncoming();
    }

    public void pokeClient(int clientID, String msg) throws TS3ServerQueryException {
        if (!this.isConnected()) {
            throw new IllegalStateException("Not connected to TS3 server!");
        }
        if (msg == null || msg.length() == 0) {
            throw new IllegalArgumentException("No message given!");
        }
        String command = "clientpoke clid=" + Integer.toString(clientID) + " msg=" + this.encodeTS3String(msg);
        HashMap<String, String> hmIn = this.doInternalCommand(command);
        if (!hmIn.get("id").equals("0")) {
            throw new TS3ServerQueryException("pokeClient()", hmIn.get("id"), hmIn.get("msg"), hmIn.get("extra_msg"), hmIn.get("failed_permid"));
        }
    }

    public void complainAdd(int clientDBID, String msg) throws TS3ServerQueryException {
        if (!this.isConnected()) {
            throw new IllegalStateException("Not connected to TS3 server!");
        }
        if (msg == null || msg.length() == 0) {
            throw new IllegalArgumentException("No message given!");
        }
        String command = "complainadd tcldbid=" + Integer.toString(clientDBID) + " message=" + this.encodeTS3String(msg);
        HashMap<String, String> hmIn = this.doInternalCommand(command);
        if (!hmIn.get("id").equals("0")) {
            throw new TS3ServerQueryException("complainAdd()", hmIn.get("id"), hmIn.get("msg"), hmIn.get("extra_msg"), hmIn.get("failed_permid"));
        }
    }

    public void complainDelete(int clientDBID, int deleteClientDBID) throws TS3ServerQueryException {
        if (!this.isConnected()) {
            throw new IllegalStateException("Not connected to TS3 server!");
        }
        String command = "complaindel tcldbid=" + Integer.toString(clientDBID) + " fcldbid=" + Integer.toString(deleteClientDBID);
        HashMap<String, String> hmIn = this.doInternalCommand(command);
        if (!hmIn.get("id").equals("0")) {
            throw new TS3ServerQueryException("complainDelete()", hmIn.get("id"), hmIn.get("msg"), hmIn.get("extra_msg"), hmIn.get("failed_permid"));
        }
    }

    public boolean isConnected() {
        if (this.socketQuery == null || this.in == null || this.out == null) {
            return false;
        }
        return this.socketQuery.isConnected();
    }

    public Vector<HashMap<String, String>> parseRawData(String rawData) {
        if (rawData == null) {
            throw new NullPointerException("rawData was null");
        }
        Vector<HashMap<String, String>> formattedData = new Vector<HashMap<String, String>>();
        StringTokenizer stEntries = new StringTokenizer(rawData, "|", false);
        while (stEntries.hasMoreTokens()) {
            formattedData.addElement(this.parseLine(stEntries.nextToken()));
        }
        return formattedData;
    }

    public Vector<Integer> searchClientDB(String search, boolean isUID) throws TS3ServerQueryException {
        if (search == null || search.length() == 0) {
            throw new IllegalArgumentException("No search string given!");
        }
        String command = "clientdbfind pattern=" + search + (isUID ? " -uid" : "");
        HashMap<String, String> hmIn = this.doInternalCommand(command);
        if (!hmIn.get("id").equals("0")) {
            throw new TS3ServerQueryException("searchClientDB()", hmIn.get("id"), hmIn.get("msg"), hmIn.get("extra_msg"), hmIn.get("failed_permid"));
        }
        if (hmIn.get("response") == null) {
            throw new IllegalStateException("No valid server response found!");
        }
        Vector<HashMap<String, String>> info = this.parseRawData(hmIn.get("response"));
        Vector<Integer> list = new Vector<Integer>();
        int i = 0;
        while (i < info.size()) {
            try {
                list.addElement(Integer.parseInt(info.elementAt(i).get("cldbid")));
            }
            catch (Exception var8_8) {
                // empty catch block
            }
            ++i;
        }
        return list;
    }

    public HashMap<String, String> getInfo(int infoMode, int objectID) throws TS3ServerQueryException {
        HashMap<String, String> hmIn;
        String command = this.getCommand(infoMode, 2);
        if (command == null) {
            throw new IllegalArgumentException("Unknown infoMode!");
        }
        if (infoMode != 11) {
            command = String.valueOf(command) + Integer.toString(objectID);
        }
        if (!(hmIn = this.doInternalCommand(command)).get("id").equals("0")) {
            throw new TS3ServerQueryException("getInfo()", hmIn.get("id"), hmIn.get("msg"), hmIn.get("extra_msg"), hmIn.get("failed_permid"));
        }
        if (hmIn.get("response") == null) {
            throw new IllegalStateException("No valid server response found!");
        }
        HashMap<String, String> info = this.parseLine(hmIn.get("response"));
        return info;
    }

    public HashMap<String, String> getPermissionInfo(int permID) throws TS3ServerQueryException {
        Vector<HashMap<String, String>> permList = this.getList(6);
        HashMap<String, String> retPermInfo = null;
        for (HashMap<String, String> permInfo : permList) {
            if (Integer.parseInt(permInfo.get("permid")) != permID) continue;
            retPermInfo = permInfo;
            break;
        }
        return retPermInfo;
    }

    public Vector<HashMap<String, String>> getPermissionList(int permListMode, int targetID) throws TS3ServerQueryException {
        String command = this.getCommand(permListMode, 3);
        if (command == null) {
            throw new IllegalArgumentException("Unknown permListMode!");
        }
        command = String.valueOf(command) + Integer.toString(targetID);
        return this.getList(command);
    }

    public Vector<HashMap<String, String>> getLogEntries(int linesCount, boolean reverse, boolean masterlog, int beginpos) throws TS3ServerQueryException {
        if (linesCount < 1 || linesCount > 100) {
            throw new IllegalArgumentException("listLimitCount has to be between 1 and 100!");
        }
        if (beginpos < 0) {
            throw new IllegalArgumentException("beginpos must be 0 or higher!");
        }
        String command = "logview lines=" + Integer.toString(linesCount) + " reverse=" + (reverse ? "1" : "0") + " instance=" + (masterlog ? "1" : "0") + " begin_pos=" + Integer.toString(beginpos);
        return this.getList(command);
    }

    public Vector<HashMap<String, String>> getList(int listMode) throws TS3ServerQueryException {
        return this.getList(listMode, null);
    }

    public Vector<HashMap<String, String>> getList(int listMode, String arguments) throws TS3ServerQueryException {
        String command = this.getCommand(listMode, 1);
        if (command == null) {
            throw new IllegalArgumentException("Unknown listMode!");
        }
        if (arguments != null && arguments.length() > 1) {
            StringTokenizer st = new StringTokenizer(arguments, ",", false);
            while (st.hasMoreTokens()) {
                String arg = st.nextToken();
                if (!this.checkListArguments(listMode, arg)) continue;
                command = String.valueOf(command) + " " + arg;
            }
        }
        return this.getList(command);
    }

    private Vector<HashMap<String, String>> getList(String command) throws TS3ServerQueryException {
        if (!this.isConnected()) {
            throw new IllegalStateException("Not connected to TS3 server!");
        }
        HashMap<String, String> hmIn = this.doInternalCommand(command);
        if (!hmIn.get("id").equals("0")) {
            throw new TS3ServerQueryException("getList()", hmIn.get("id"), hmIn.get("msg"), hmIn.get("extra_msg"), hmIn.get("failed_permid"));
        }
        if (hmIn.get("response") == null) {
            throw new IllegalStateException("No valid server response found!");
        }
        Vector<HashMap<String, String>> list = this.parseRawData(hmIn.get("response"));
        return list;
    }

    private HashMap<String, String> readIncoming() {
        String temp;
        String inData = "";
        HashMap<String, String> hmIn = new HashMap<>();
        if (!this.isConnected()) {
            throw new IllegalStateException("Not connected to TS3 server!");
        }
        do {
            try {
                temp = this.in.readLine();
                this.writeCommLog("< " + temp);
            }
            catch (SocketTimeoutException e1) {
                this.closeTS3Connection();
                throw new IllegalStateException("Closed TS3 Connection: " + e1.toString(), e1);
            }
            catch (SocketException e2) {
                this.closeTS3Connection();
                throw new IllegalStateException("Closed TS3 Connection: " + e2.toString(), e2);
            }
            catch (Exception e) {
                throw new IllegalStateException("Unknown exception: " + e.toString(), e);
            }
            if (temp == null) {
                this.closeTS3Connection();
                throw new IllegalStateException("null object, maybe connection to TS3 server interrupted.");
            }
            if (temp.startsWith("error ")) break;
            if (temp.length() <= 2 || this.handleAction(temp)) continue;
            if (inData.length() != 0) {
                inData = String.valueOf(inData) + System.getProperty("line.separator", "\n");
            }
            inData = String.valueOf(inData) + temp;
        } while (true);
        hmIn = this.parseLine(temp);
        if (hmIn == null) {
            throw new IllegalStateException("null object, maybe connection to TS3 server interrupted.");
        }
        hmIn.put("response", inData);
        this.eventNotifyCheckActive = true;
        return hmIn;
    }

    public String encodeTS3String(String str) {
        str = str.replace("\\", "\\\\");
        str = str.replace(" ", "\\s");
        str = str.replace("/", "\\/");
        str = str.replace("|", "\\p");
        str = str.replace("\b", "\\b");
        str = str.replace("\f", "\\f");
        str = str.replace("\n", "\\n");
        str = str.replace("\r", "\\r");
        str = str.replace("\t", "\\t");
        Character cBell = new Character('\u0007');
        Character cVTab = new Character('\u000b');
        str = str.replace(cBell.toString(), "\\a");
        str = str.replace(cVTab.toString(), "\\v");
        return str;
    }

    public String decodeTS3String(String str) {
        str = str.replace("\\\\", "\\[$mksave]");
        str = str.replace("\\s", " ");
        str = str.replace("\\/", "/");
        str = str.replace("\\p", "|");
        str = str.replace("\\b", "\b");
        str = str.replace("\\f", "\f");
        str = str.replace("\\n", "\n");
        str = str.replace("\\r", "\r");
        str = str.replace("\\t", "\t");
        Character cBell = new Character('\u0007');
        Character cVTab = new Character('\u000b');
        str = str.replace("\\a", cBell.toString());
        str = str.replace("\\v", cVTab.toString());
        str = str.replace("\\[$mksave]", "\\");
        return str;
    }

    public HashMap<String, String> parseLine(String line) {
        if (line == null || line.length() == 0) {
            return null;
        }
        StringTokenizer st = new StringTokenizer(line, " ", false);
        HashMap<String, String> retValue = new HashMap<String, String>();
        int pos = -1;
        while (st.hasMoreTokens()) {
            String temp = st.nextToken();
            pos = temp.indexOf("=");
            if (pos == -1) {
                retValue.put(temp, "");
                continue;
            }
            String key = temp.substring(0, pos);
            retValue.put(key, this.decodeTS3String(temp.substring(pos + 1)));
        }
        return retValue;
    }

    private boolean checkListArguments(int listMode, String argument) {
        if (listMode == 2) {
            if (argument.equalsIgnoreCase("-topic")) {
                return true;
            }
            if (argument.equalsIgnoreCase("-flags")) {
                return true;
            }
            if (argument.equalsIgnoreCase("-voice")) {
                return true;
            }
            if (argument.equalsIgnoreCase("-limits")) {
                return true;
            }
            if (argument.equalsIgnoreCase("-icon")) {
                return true;
            }
            if (argument.equalsIgnoreCase("-secondsempty")) {
                return true;
            }
        }
        if (listMode == 1) {
            if (argument.equalsIgnoreCase("-uid")) {
                return true;
            }
            if (argument.equalsIgnoreCase("-away")) {
                return true;
            }
            if (argument.equalsIgnoreCase("-voice")) {
                return true;
            }
            if (argument.equalsIgnoreCase("-times")) {
                return true;
            }
            if (argument.equalsIgnoreCase("-groups")) {
                return true;
            }
            if (argument.equalsIgnoreCase("-info")) {
                return true;
            }
            if (argument.equalsIgnoreCase("-icon")) {
                return true;
            }
            if (argument.equalsIgnoreCase("-country")) {
                return true;
            }
            if (argument.equalsIgnoreCase("-ip")) {
                return true;
            }
        }
        if (listMode == 3) {
            if (argument.equalsIgnoreCase("-uid")) {
                return true;
            }
            if (argument.equalsIgnoreCase("-all")) {
                return true;
            }
            if (argument.equalsIgnoreCase("-short")) {
                return true;
            }
            if (argument.equalsIgnoreCase("-onlyoffline")) {
                return true;
            }
        }
        if (listMode == 5) {
            if (argument.startsWith("start=") && argument.indexOf(" ") == -1) {
                return true;
            }
            if (argument.startsWith("duration=") && argument.indexOf(" ") == -1) {
                return true;
            }
            if (argument.equalsIgnoreCase("-count")) {
                return true;
            }
        }
        if (listMode == 8 && argument.startsWith("tcldbid=") && argument.indexOf(" ") == -1) {
            return true;
        }
        if (listMode == 9) {
            if (argument.startsWith("sgid=") && argument.indexOf(" ") == -1) {
                return true;
            }
            if (argument.equalsIgnoreCase("-names")) {
                return true;
            }
        }
        return false;
    }

    private String getCommand(int mode, int listType) {
        if (listType == 1) {
            if (mode == 2) {
                return "channellist";
            }
            if (mode == 5) {
                return "clientdblist";
            }
            if (mode == 1) {
                return "clientlist";
            }
            if (mode == 6) {
                return "permissionlist";
            }
            if (mode == 4) {
                return "servergrouplist";
            }
            if (mode == 3) {
                return "serverlist";
            }
            if (mode == 7) {
                return "banlist";
            }
            if (mode == 8) {
                return "complainlist";
            }
            if (mode == 9) {
                return "servergroupclientlist";
            }
        } else if (listType == 2) {
            if (mode == 11) {
                return "serverinfo";
            }
            if (mode == 12) {
                return "channelinfo cid=";
            }
            if (mode == 13) {
                return "clientinfo clid=";
            }
            if (mode == 14) {
                return "clientdbinfo cldbid=";
            }
        } else if (listType == 3) {
            if (mode == 21) {
                return "channelpermlist cid=";
            }
            if (mode == 23) {
                return "clientpermlist cldbid=";
            }
            if (mode == 22) {
                return "servergrouppermlist sgid=";
            }
        }
        return null;
    }

    private boolean handleAction(final String actionLine) {
        int pos;
        if (!actionLine.startsWith("notify")) {
            return false;
        }
        if (this.actionClass != null && (pos = actionLine.indexOf(" ")) != -1) {
            final String eventType = actionLine.substring(0, pos);
            Thread t = new Thread(new Runnable(){

                public void run() {
                    try {
                        JTS3ServerQuery.this.actionClass.teamspeakActionPerformed(eventType, JTS3ServerQuery.this.parseLine(actionLine.substring(pos + 1)));
                    }
                    catch (Exception e) {
                        JTS3ServerQuery.this.writeErrLog(e);
                    }
                }
            });
            t.setName(String.valueOf(this.threadName) + "handleAction");
            t.start();
        }
        return true;
    }

}

