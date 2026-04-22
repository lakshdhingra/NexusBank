import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.text.*;
import java.util.*;
import java.util.List;
public class NexusBank {
    // --- Color Constants ---
    static final String F="Segoe UI";
    static final int FB=13,FH=20,FL=11;
    static final Color L_BG   =new Color(0xEEF2FB),L_CARD =new Color(0xFFFFFF);
    static final Color L_TEXT =new Color(0x0D1B3E),L_SUB  =new Color(0x5A6A8A);
    static final Color L_BORD =new Color(0xD4DCF0),L_ROW  =new Color(0xF4F7FF);
    static final Color D_BG   =new Color(0x08101E),D_CARD =new Color(0x0F1C30);
    static final Color D_TEXT =new Color(0xF8FAFF),D_SUB  =new Color(0x8FA8CC);
    static final Color D_BORD =new Color(0x1C2D45),D_ROW  =new Color(0x0C1828);
    static final Color BLUE   =new Color(0x2563EB),BLUE_H=new Color(0x1D4ED8),BLUE_D=new Color(0x60A5FA);
    static final Color GREEN  =new Color(0x059669),RED   =new Color(0xDC2626),GOLD=new Color(0xD97706);
    static final Color PURPLE =new Color(0x7C3AED),TEAL  =new Color(0x0891B2),ROSE=new Color(0xE11D48);
    // --- Account ---
    static class Account {
        String id,owner,type,status,city; double balance;
        Account(String id,String o,String t,double b,String city){
            this.id=id;owner=o;type=t;balance=b;this.city=city;status="Active";}
    }
    // --- Transaction ---
    static class Transaction {
        String date,from,to,type,desc; double amount;
        Transaction(String dt,String f,String t,double a,String tp,String d){
            date=dt;from=f;to=t;amount=a;type=tp;desc=d;}
    }

    // --- SupabaseService ---
    static class SupabaseService {
        private final String BASE_URL = "https://wwnabotcvzwzawejpnem.supabase.co/rest/v1";
        private final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Ind3bmFib3Rjdnp3emF3ZWpwbmVtIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzY4Mzk1OTUsImV4cCI6MjA5MjQxNTU5NX0.t9OOzdBUiWbp4MPPq3sKi1ItNBAVEPde0PD8pbambHw";

        private java.net.HttpURLConnection getConnection(String endpoint, String method) throws Exception {
            java.net.URL url = new java.net.URL(BASE_URL + endpoint);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);
            conn.setRequestProperty("apikey", API_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Prefer", "return=representation");
            return conn;
        }

        public List<Account> fetchAccounts() {
            List<Account> list = new ArrayList<>();
            try {
                java.net.HttpURLConnection conn = getConnection("/accounts", "GET");
                java.util.Scanner scanner = new java.util.Scanner(conn.getInputStream());
                String response = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
                scanner.close();
                response = response.trim();
                if (response.startsWith("[")) response = response.substring(1, response.length() - 1);
                String[] objects = response.split("\\},\\{");
                for (String obj : objects) {
                    if (obj.trim().isEmpty()) continue;
                    obj = obj.replace("{", "").replace("}", "");
                    String id = extractStr(obj, "id");
                    String owner = extractStr(obj, "owner");
                    String type = extractStr(obj, "type");
                    double balance = Double.parseDouble(extractNum(obj, "balance"));
                    String city = extractStr(obj, "city");
                    String status = extractStr(obj, "status");
                    Account acc = new Account(id, owner, type, balance, city);
                    if (status != null && !status.isEmpty()) acc.status = status;
                    list.add(acc);
                }
            } catch (Exception e) { e.printStackTrace(); }
            return list;
        }

        public List<Transaction> fetchTransactions() {
            List<Transaction> list = new ArrayList<>();
            try {
                java.net.HttpURLConnection conn = getConnection("/transactions", "GET");
                java.util.Scanner scanner = new java.util.Scanner(conn.getInputStream());
                String response = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
                scanner.close();
                response = response.trim();
                if (response.startsWith("[")) response = response.substring(1, response.length() - 1);
                String[] objects = response.split("\\},\\{");
                for (String obj : objects) {
                    if (obj.trim().isEmpty()) continue;
                    obj = obj.replace("{", "").replace("}", "");
                    String date = extractStr(obj, "date");
                    String from = extractStr(obj, "from_account");
                    String to = extractStr(obj, "to_account");
                    double amount = Double.parseDouble(extractNum(obj, "amount"));
                    String type = extractStr(obj, "type");
                    String desc = extractStr(obj, "description");
                    list.add(new Transaction(date, from, to, amount, type, desc));
                }
            } catch (Exception e) { e.printStackTrace(); }
            return list;
        }

        public void insertAccount(Account acc) {
            try {
                java.net.HttpURLConnection conn = getConnection("/accounts", "POST");
                conn.setDoOutput(true);
                String json = String.format("{\"id\":\"%s\",\"owner\":\"%s\",\"type\":\"%s\",\"balance\":%f,\"city\":\"%s\",\"status\":\"%s\"}",
                        acc.id, acc.owner, acc.type, acc.balance, acc.city, acc.status);
                try (java.io.OutputStream os = conn.getOutputStream()) { os.write(json.getBytes()); os.flush(); }
                conn.getResponseCode();
            } catch (Exception e) { e.printStackTrace(); }
        }

        public void updateBalance(String accountId, double newBalance) {
            try {
                java.net.HttpURLConnection conn = getConnection("/accounts?id=eq." + accountId, "PATCH");
                conn.setDoOutput(true);
                String json = String.format("{\"balance\":%f}", newBalance);
                try (java.io.OutputStream os = conn.getOutputStream()) { os.write(json.getBytes()); os.flush(); }
                conn.getResponseCode();
            } catch (Exception e) { e.printStackTrace(); }
        }

        public void insertTransaction(Transaction t) {
            try {
                java.net.HttpURLConnection conn = getConnection("/transactions", "POST");
                conn.setDoOutput(true);
                String json = String.format("{\"date\":\"%s\",\"from_account\":\"%s\",\"to_account\":\"%s\",\"amount\":%f,\"type\":\"%s\",\"description\":\"%s\"}",
                        t.date, t.from, t.to, t.amount, t.type, t.desc);
                try (java.io.OutputStream os = conn.getOutputStream()) { os.write(json.getBytes()); os.flush(); }
                conn.getResponseCode();
            } catch (Exception e) { e.printStackTrace(); }
        }

        private String extractStr(String json, String key) {
            int idx = json.indexOf("\"" + key + "\":");
            if (idx == -1) return "";
            int start = json.indexOf("\"", idx + key.length() + 2);
            if (start == -1) return "";
            int end = json.indexOf("\"", start + 1);
            if (end == -1) return "";
            return json.substring(start + 1, end);
        }

        private String extractNum(String json, String key) {
            int idx = json.indexOf("\"" + key + "\":");
            if (idx == -1) return "0";
            int start = idx + key.length() + 2;
            int end = json.indexOf(",", start);
            if (end == -1) end = json.length();
            return json.substring(start, end).trim();
        }
    }
    // --- DataStore ---
    static class DataStore {
        SupabaseService api = new SupabaseService();
        List<Account> accounts=new ArrayList<>();
        List<Transaction> txns=new ArrayList<>();
        int ctr=5;
        static final SimpleDateFormat SDF=new SimpleDateFormat("dd-MM-yyyy");
        void seed(){
            accounts = api.fetchAccounts();
            txns = api.fetchTransactions();
        }
        String ago(int d){Calendar c=Calendar.getInstance();c.add(Calendar.DAY_OF_YEAR,-d);return SDF.format(c.getTime());}
        void addT(String dt,String f,String t,double a,String tp,String d){txns.add(new Transaction(dt,f,t,a,tp,d));}
        Account find(String id){for(Account a:accounts)if(a.id.equals(id))return a;return null;}
        String nextId(){return String.format("ACC-%03d",ctr++);}
        double totalBal(){double s=0;for(Account a:accounts)s+=a.balance;return s;}
        long recentTx(){Calendar c=Calendar.getInstance();c.add(Calendar.DAY_OF_YEAR,-30);long n=0;
            for(Transaction t:txns){try{if(SDF.parse(t.date).after(c.getTime()))n++;}catch(Exception e){}}return n;}
        double monthSpend(){Calendar c=Calendar.getInstance();c.add(Calendar.DAY_OF_YEAR,-30);double s=0;
            for(Transaction t:txns){try{if("Debit".equals(t.type)&&SDF.parse(t.date).after(c.getTime()))s+=t.amount;}catch(Exception e){}}return s;}
        double monthIncome(){Calendar c=Calendar.getInstance();c.add(Calendar.DAY_OF_YEAR,-30);double s=0;
            for(Transaction t:txns){try{if("Credit".equals(t.type)&&SDF.parse(t.date).after(c.getTime()))s+=t.amount;}catch(Exception e){}}return s;}
        List<Transaction> forAccount(String id){List<Transaction>r=new ArrayList<>();
            for(Transaction t:txns)if(t.from.equals(id)||t.to.equals(id))r.add(t);return r;}
        double[]weeklySpend(){double[]w=new double[6];Calendar c=Calendar.getInstance();
            for(Transaction t:txns){try{if(!"Debit".equals(t.type))continue;
                long diff=(c.getTimeInMillis()-SDF.parse(t.date).getTime())/(1000*60*60*24);
                int b=(int)(diff/5);if(b<6)w[5-b]+=t.amount;}catch(Exception e){}}return w;}
    }
    // --- ThemeManager ---
    static class ThemeManager {
        boolean dark=false;
        Color bg()  {return dark?D_BG:L_BG;}   Color card(){return dark?D_CARD:L_CARD;}
        Color text(){return dark?D_TEXT:L_TEXT;} Color sub() {return dark?D_SUB:L_SUB;}
        Color bord(){return dark?D_BORD:L_BORD;} Color row() {return dark?D_ROW:L_ROW;}
        Color acc() {return dark?BLUE_D:BLUE;}
    }
    // --- GradientPanel ---
    static class GradientPanel extends JPanel {
        Color c1,c2;
        GradientPanel(Color c1,Color c2){this.c1=c1;this.c2=c2;setOpaque(false);}
        @Override protected void paintComponent(Graphics g){
            Graphics2D g2=(Graphics2D)g.create();
            g2.setPaint(new GradientPaint(0,0,c1,getWidth(),0,c2));
            g2.fillRect(0,0,getWidth(),getHeight());g2.dispose();
        }
    }
    // --- RoundedPanel ---
    static class RoundedPanel extends JPanel {
        int r; Color bg,bord; float alpha=1f; boolean shadow;
        RoundedPanel(int r,Color bg,Color bord,boolean sh){this.r=r;this.bg=bg;this.bord=bord;this.shadow=sh;setOpaque(false);}
        RoundedPanel(int r,Color bg,Color bord){this(r,bg,bord,false);}
        @Override protected void paintComponent(Graphics g){
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            if(shadow){g2.setColor(new Color(0,0,0,20));g2.fillRoundRect(3,6,getWidth()-4,getHeight()-4,r,r);}
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,alpha));
            g2.setColor(bg);g2.fillRoundRect(0,0,getWidth(),getHeight(),r,r);
            g2.setColor(bord);g2.setStroke(new BasicStroke(1.3f));
            g2.drawRoundRect(1,1,getWidth()-2,getHeight()-2,r,r);
            g2.dispose();super.paintComponent(g);
        }
    }
    // --- PulseButton ---
    static class PulseButton extends JButton {
        Color base,hov; boolean over=false,outline=false; float scale=1f;
        PulseButton(String t,Color b){super(t);base=b;hov=b.darker();
            setForeground(Color.WHITE);setFocusPainted(false);setBorderPainted(false);
            setFont(new Font(F,Font.BOLD,FB));setCursor(new Cursor(Cursor.HAND_CURSOR));
            setContentAreaFilled(false);setOpaque(false);setBorder(new EmptyBorder(10,22,10,22));
            addMouseListener(new MouseAdapter(){
                public void mouseEntered(MouseEvent e){over=true;spring(1.05f);}
                public void mouseExited(MouseEvent e){over=false;spring(1f);}});}
        PulseButton(String t,Color b,boolean ol){this(t,b);outline=ol;setForeground(b);}
        void spring(float target){javax.swing.Timer tm=new javax.swing.Timer(8,null);float[]v={scale};
            tm.addActionListener(e->{v[0]+=(target-v[0])*0.28f;scale=v[0];repaint();
                if(Math.abs(v[0]-target)<0.001f){scale=target;((javax.swing.Timer)e.getSource()).stop();}});tm.start();}
        @Override protected void paintComponent(Graphics g){
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            int w=getWidth(),h=getHeight(),sw=(int)(w*scale),sh=(int)(h*scale),ox=(w-sw)/2,oy=(h-sh)/2;
            if(over&&!outline){g2.setColor(new Color(base.getRed(),base.getGreen(),base.getBlue(),30));
                g2.fillRoundRect(ox-6,oy-6,sw+12,sh+12,16,16);}
            if(outline){if(over){g2.setColor(new Color(base.getRed(),base.getGreen(),base.getBlue(),22));
                    g2.fillRoundRect(ox,oy,sw,sh,12,12);}
                g2.setColor(base);g2.setStroke(new BasicStroke(1.6f));g2.drawRoundRect(ox+1,oy+1,sw-2,sh-2,12,12);}
            else{g2.setColor(over?hov:base);g2.fillRoundRect(ox,oy,sw,sh,12,12);}
            g2.dispose();super.paintComponent(g);
        }
    }
    // --- BadgeLabel ---
    static class BadgeLabel extends JLabel {
        Color bg;
        BadgeLabel(String t,Color bg){super(t);this.bg=bg;setForeground(Color.WHITE);
            setFont(new Font(F,Font.BOLD,FL));setOpaque(false);setHorizontalAlignment(CENTER);}
        @Override protected void paintComponent(Graphics g){
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg);g2.fillRoundRect(0,0,getWidth(),getHeight(),12,12);
            g2.dispose();super.paintComponent(g);
        }
    }
    // --- SparkBar ---
    static class SparkBar extends JPanel {
        double[]values; Color barColor,labelColor; String[]labels;
        SparkBar(double[]v,String[]l,Color bar,Color lc){values=v;labels=l;barColor=bar;labelColor=lc;setOpaque(false);}
        @Override protected void paintComponent(Graphics g){
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            int w=getWidth(),h=getHeight()-22,pad=6;
            if(values==null||values.length==0){g2.dispose();return;}
            double max=0;for(double v:values)if(v>max)max=v;if(max==0)max=1;
            int bw=Math.max(1,(w-pad*(values.length+1))/values.length);
            for(int i=0;i<values.length;i++){
                int bh=(int)(values[i]/max*(h-8));int x=pad+(bw+pad)*i,y=h-bh+2;
                g2.setColor(new Color(barColor.getRed(),barColor.getGreen(),barColor.getBlue(),40));
                g2.fillRoundRect(x,2,bw,h-2,6,6);
                g2.setColor(barColor);g2.fillRoundRect(x,y,bw,bh,6,6);
                if(labels!=null&&i<labels.length){
                    g2.setFont(new Font(F,Font.PLAIN,9));g2.setColor(labelColor);
                    FontMetrics fm=g2.getFontMetrics();
                    g2.drawString(labels[i],x+(bw-fm.stringWidth(labels[i]))/2,h+16);}
            }
            g2.dispose();
        }
    }
    // --- NetWorthBar ---
    static class NetWorthBar extends JPanel {
        double savings,current,fd; ThemeManager tm;
        NetWorthBar(double s,double c,double f,ThemeManager tm){savings=s;current=c;fd=f;this.tm=tm;setOpaque(false);setPreferredSize(new Dimension(0,56));}
        @Override protected void paintComponent(Graphics g){
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            double tot=savings+current+fd;if(tot==0)tot=1;
            int w=getWidth()-20,h=20,y=18;
            g2.setColor(tm.bord());g2.fillRoundRect(10,y,w,h,h,h);
            int sw=(int)(savings/tot*w);g2.setColor(BLUE_D);g2.fillRoundRect(10,y,sw,h,h,h);
            int cw=(int)(current/tot*w);if(cw>0){g2.setColor(PURPLE);g2.fillRoundRect(10+sw,y,cw,h,h,h);}
            if(w-sw-cw>0){g2.setColor(GOLD);g2.fillRoundRect(10+sw+cw,y,w-sw-cw,h,h,h);}
            g2.setFont(new Font(F,Font.BOLD,9));g2.setColor(Color.WHITE);
            if(sw>45)g2.drawString("Savings",16,y+13);
            if(cw>48)g2.drawString("Current",10+sw+4,y+13);
            if(w-sw-cw>36)g2.drawString("FD",10+sw+cw+4,y+13);
            g2.dispose();
        }
    }
    // --- StatCard ---
    static class StatCard extends RoundedPanel {
        JLabel titleLbl,valueLbl,changeLbl; Color accent; float[]anim={0f};
        JPanel bar;
        StatCard(String title,String val,String icon,Color accent,ThemeManager tm){
            super(18,tm.card(),tm.bord(),true);this.accent=accent;
            setLayout(new BorderLayout(0,0));setBorder(new EmptyBorder(20,22,16,22));
            JPanel top=new JPanel(new BorderLayout());top.setOpaque(false);
            JLabel ico=new JLabel(icon);ico.setFont(new Font("Dialog",Font.PLAIN,24));ico.setForeground(accent);
            titleLbl=new JLabel(title);titleLbl.setFont(new Font(F,Font.BOLD,FL+1));titleLbl.setForeground(tm.sub());
            valueLbl=new JLabel(val);valueLbl.setFont(new Font(F,Font.BOLD,24));valueLbl.setForeground(tm.text());
            bar=new JPanel(){@Override protected void paintComponent(Graphics g){
                Graphics2D g2=(Graphics2D)g.create();
                g2.setColor(new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),35));g2.fillRoundRect(0,2,getWidth(),4,4,4);
                g2.setColor(accent);g2.fillRoundRect(0,2,(int)(getWidth()*anim[0]),4,4,4);g2.dispose();}};
            bar.setOpaque(false);bar.setPreferredSize(new Dimension(0,10));
            changeLbl=new JLabel(" ");changeLbl.setFont(new Font(F,Font.PLAIN,FL));changeLbl.setForeground(GREEN);
            JPanel icoWrap=new JPanel(new FlowLayout(FlowLayout.RIGHT,0,2));icoWrap.setOpaque(false);icoWrap.add(ico);
            top.add(titleLbl,BorderLayout.CENTER);top.add(icoWrap,BorderLayout.EAST);
            JPanel mid=new JPanel(new BorderLayout());mid.setOpaque(false);mid.add(valueLbl,BorderLayout.CENTER);
            JPanel bot=new JPanel(new BorderLayout(0,2));bot.setOpaque(false);
            bot.add(bar,BorderLayout.NORTH);bot.add(changeLbl,BorderLayout.SOUTH);
            add(top,BorderLayout.NORTH);add(mid,BorderLayout.CENTER);add(bot,BorderLayout.SOUTH);
            javax.swing.Timer t=new javax.swing.Timer(12,null);
            t.addActionListener(e->{anim[0]=Math.min(1f,anim[0]+0.045f);alpha=anim[0];bar.repaint();repaint();
                if(anim[0]>=1f)((javax.swing.Timer)e.getSource()).stop();});t.start();
        }
        void refresh(String v,String ch,ThemeManager tm){
            valueLbl.setText(v);valueLbl.setForeground(tm.text());
            titleLbl.setForeground(tm.sub());
            if(ch!=null){changeLbl.setText(ch);}
            bg=tm.card();bord=tm.bord();repaint();
        }
        // called by applyTheme to ensure all child labels get correct colors
        void applyColors(ThemeManager tm){
            titleLbl.setForeground(tm.sub());valueLbl.setForeground(tm.text());
            bg=tm.card();bord=tm.bord();repaint();
        }
    }
    // --- StyledTable ---
    static class StyledTable extends JTable {
        ThemeManager tm;
        StyledTable(TableModel m,ThemeManager tm){super(m);this.tm=tm;
            setRowHeight(34);setShowGrid(false);setIntercellSpacing(new Dimension(0,0));
            setFont(new Font(F,Font.PLAIN,FB));setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            setFillsViewportHeight(true);getTableHeader().setReorderingAllowed(false);
            getTableHeader().setFont(new Font(F,Font.BOLD,FL+1));
            getTableHeader().setBorder(BorderFactory.createEmptyBorder());
            getTableHeader().setPreferredSize(new Dimension(0,38));}
        void theme(){
            setBackground(tm.card());setForeground(tm.text());
            setSelectionBackground(tm.acc());setSelectionForeground(Color.WHITE);
            getTableHeader().setBackground(tm.acc());getTableHeader().setForeground(Color.WHITE);repaint();}
        @Override public Component prepareRenderer(TableCellRenderer r,int row,int col){
            Component c=super.prepareRenderer(r,row,col);
            if(!isRowSelected(row)){c.setBackground(row%2==0?tm.card():tm.row());c.setForeground(tm.text());}
            if(c instanceof JLabel)((JLabel)c).setBorder(new EmptyBorder(0,12,0,12));return c;}
    }
    // --- Toast ---
    static class Toast extends JWindow {
        Toast(JFrame parent,String msg,Color accent){
            super(parent);
            JPanel p=new JPanel(new FlowLayout(FlowLayout.LEFT,12,9));p.setBackground(accent);
            p.setBorder(new LineBorder(accent.darker(),1,true));
            JLabel ico=new JLabel(accent.equals(RED)||accent.equals(ROSE)?"✗":"✓");
            ico.setFont(new Font(F,Font.BOLD,13));ico.setForeground(Color.WHITE);
            JLabel lbl=new JLabel("<html><span style='font-family:Segoe UI;font-size:12px;color:white'>"+msg+"</span></html>");
            p.add(ico);p.add(lbl);setContentPane(p);pack();
            Point loc=parent.getLocationOnScreen();
            setLocation(loc.x+parent.getWidth()-getWidth()-20,loc.y+parent.getHeight()-getHeight()-40);
            setVisible(true);float[]op={0f};
            javax.swing.Timer t=new javax.swing.Timer(18,null);
            t.addActionListener(e->{op[0]+=0.08f;if(op[0]>=1f){op[0]=1f;((javax.swing.Timer)e.getSource()).stop();
                new javax.swing.Timer(2400,ev->{dismiss();((javax.swing.Timer)ev.getSource()).stop();}).start();}
                try{setOpacity(Math.min(op[0],1f));}catch(Exception ig){}});
            try{setOpacity(0f);}catch(Exception ig){}t.start();}
        void dismiss(){float[]op={1f};javax.swing.Timer t=new javax.swing.Timer(18,null);
            t.addActionListener(e->{op[0]-=0.1f;if(op[0]<=0){dispose();((javax.swing.Timer)e.getSource()).stop();}
                try{setOpacity(Math.max(op[0],0f));}catch(Exception ig){}});t.start();}
    }
    // === App Fields ===
    DataStore ds=new DataStore(); ThemeManager tm=new ThemeManager();
    JFrame frame; JLabel statusBar,statusDot; JTabbedPane tabs;
    DefaultTableModel accMdl,txMdl; StyledTable accTable;
    JComboBox<String> txFilt,srcCmb,dstCmb,cbType;
    JTextField tfOwner,tfDeposit,tfAmount,tfDesc,tfSearch,tfQuick;
    JLabel eOwner,eDeposit,eAmount,eSrc,eDst,eQuick;
    PulseButton btnXfer,btnCreate;
    List<StyledTable> tables=new ArrayList<>();
    List<RoundedPanel> allCards=new ArrayList<>();
    StatCard[]statCards=new StatCard[4];
    // track all themed labels so applyTheme can update them precisely
    List<JLabel> subLabels=new ArrayList<>();   // muted/sub color
    List<JLabel> textLabels=new ArrayList<>();  // primary text color
    JPanel rootPanel; GradientPanel toolbar; JLabel logoLbl; PulseButton themeBtn;
    JLabel detailId,detailOwner,detailType,detailBal,detailStatus,detailCity;
    SparkBar sparkBar; NetWorthBar nwBar;
    // --- Build ---
    void build(){
        frame=new JFrame("Nexus Bank — India");frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1240,790);frame.setMinimumSize(new Dimension(1020,660));frame.setLocationRelativeTo(null);
        rootPanel=new JPanel(new BorderLayout());rootPanel.setBackground(tm.bg());
        // --- Toolbar ---
        toolbar=new GradientPanel(new Color(0x0F2460),new Color(0x1A3A8F));
        toolbar.setLayout(new BorderLayout());toolbar.setBorder(new EmptyBorder(0,24,0,24));
        toolbar.setPreferredSize(new Dimension(0,66));
        JPanel logoBox=new JPanel();logoBox.setOpaque(false);logoBox.setLayout(new BoxLayout(logoBox,BoxLayout.Y_AXIS));
        logoLbl=new JLabel("  \u20B9 NEXUS BANK");
        logoLbl.setFont(new Font(F,Font.BOLD,FH+4));logoLbl.setForeground(Color.WHITE);
        JLabel tag=new JLabel("  Aapka Bharosa, Hamaari Zimmedari  \u2014  Scheduled Commercial Bank");
        tag.setFont(new Font(F,Font.ITALIC,FL));tag.setForeground(new Color(0xBDD4FF));
        logoBox.add(Box.createVerticalStrut(12));logoBox.add(logoLbl);logoBox.add(tag);logoBox.add(Box.createVerticalStrut(12));
        JPanel topRight=new JPanel(new FlowLayout(FlowLayout.RIGHT,14,0));topRight.setOpaque(false);
        JLabel clock=new JLabel();clock.setFont(new Font(F,Font.PLAIN,FL+1));clock.setForeground(new Color(0xBDD4FF));
        new javax.swing.Timer(1000,e->clock.setText(new SimpleDateFormat("EEE dd MMM  HH:mm:ss").format(new Date()))).start();
        themeBtn=new PulseButton("◐  Dark Mode",Color.WHITE,true);
        themeBtn.setForeground(Color.WHITE);themeBtn.base=Color.WHITE;themeBtn.hov=new Color(0xCCDDFF);
        themeBtn.addActionListener(e->toggleTheme());
        topRight.add(clock);topRight.add(themeBtn);
        toolbar.add(logoBox,BorderLayout.WEST);toolbar.add(topRight,BorderLayout.EAST);
        // --- Tabs ---
        tabs=new JTabbedPane(JTabbedPane.TOP);tabs.setFont(new Font(F,Font.PLAIN,FB));
        tabs.addTab("  Dashboard  ",mkDashboard());
        tabs.addTab("  Accounts  ",mkAccounts());
        tabs.addTab("  Transactions  ",mkTransactions());
        tabs.addTab("  Transfer  ",mkTransfer());
        tabs.addTab("  New Account  ",mkNewAccount());
        tabs.addTab("  Reports  ",mkReports());
        // --- Status bar ---
        JPanel sb=new JPanel(new BorderLayout(4,0));sb.setPreferredSize(new Dimension(0,30));
        statusDot=new JLabel("●");statusDot.setFont(new Font(F,Font.PLAIN,10));
        statusDot.setForeground(GREEN);statusDot.setBorder(new EmptyBorder(0,14,0,4));
        statusBar=new JLabel("System ready — Welcome to Nexus Bank");
        statusBar.setFont(new Font(F,Font.PLAIN,FL+1));
        JLabel ver=new JLabel("IFSC: NXBK0000001  |  RBI Regulated  ");
        ver.setFont(new Font(F,Font.PLAIN,FL));
        sb.add(statusDot,BorderLayout.WEST);sb.add(statusBar,BorderLayout.CENTER);sb.add(ver,BorderLayout.EAST);
        textLabels.add(statusBar);subLabels.add(ver);
        rootPanel.add(toolbar,BorderLayout.NORTH);
        rootPanel.add(tabs,BorderLayout.CENTER);
        rootPanel.add(sb,BorderLayout.SOUTH);
        frame.setContentPane(rootPanel);
        applyTheme();frame.setVisible(true);
    }
    void toggleTheme(){tm.dark=!tm.dark;
        themeBtn.setText(tm.dark?"☀  Light Mode":"◐  Dark Mode");
        if(tm.dark){toolbar.c1=new Color(0x060D1A);toolbar.c2=new Color(0x0A152A);}
        else{toolbar.c1=new Color(0x0F2460);toolbar.c2=new Color(0x1A3A8F);}
        applyTheme();}
    // --- Dashboard ---
    JPanel mkDashboard(){
        JPanel p=new JPanel(new BorderLayout(0,18));p.setOpaque(false);p.setBorder(new EmptyBorder(24,26,20,26));
        JLabel h=new JLabel("Dashboard");h.setFont(new Font(F,Font.BOLD,FH+4));h.setForeground(tm.text());
        JLabel hs=new JLabel("Your financial overview — RBI Compliant Banking Portal");
        hs.setFont(new Font(F,Font.PLAIN,FL+1));hs.setForeground(tm.sub());
        JPanel hRow=new JPanel(new BorderLayout(0,3));hRow.setOpaque(false);hRow.setBorder(new EmptyBorder(0,0,6,0));
        hRow.add(h,BorderLayout.NORTH);hRow.add(hs,BorderLayout.SOUTH);
        textLabels.add(h);subLabels.add(hs);
        Color[]ac={BLUE,GREEN,PURPLE,GOLD};
        String[]ti={"Total Portfolio","Active Accounts","Transactions (30d)","Monthly Debit"};
        String[]va={inr(ds.totalBal()),""+ds.accounts.size(),""+ds.recentTx(),inr(ds.monthSpend())};
        String[]ic={"₹","🏦","📊","💳"};
        String[]ch={"All accounts combined",ds.accounts.size()+" registered","Past 30 days","UPI+NEFT+IMPS"};
        JPanel grid=new JPanel(new GridLayout(1,4,16,0));grid.setOpaque(false);
        for(int i=0;i<4;i++){statCards[i]=new StatCard(ti[i],va[i],ic[i],ac[i],tm);
            statCards[i].changeLbl.setText(ch[i]);grid.add(statCards[i]);allCards.add(statCards[i]);}
        JPanel mid=new JPanel(new GridLayout(1,2,18,0));mid.setOpaque(false);mid.setPreferredSize(new Dimension(0,240));
        // Spend chart card
        RoundedPanel cc=new RoundedPanel(16,tm.card(),tm.bord(),true);cc.setLayout(new BorderLayout(0,6));
        cc.setBorder(new EmptyBorder(18,20,12,20));allCards.add(cc);
        JLabel cT=new JLabel("Weekly Debit Trend");cT.setFont(new Font(F,Font.BOLD,FL+3));cT.setForeground(tm.text());
        JLabel cS=new JLabel("Grouped in 5-day buckets");cS.setFont(new Font(F,Font.PLAIN,FL));cS.setForeground(tm.sub());
        JPanel cHdr=new JPanel(new BorderLayout(0,2));cHdr.setOpaque(false);cHdr.add(cT,BorderLayout.NORTH);cHdr.add(cS,BorderLayout.SOUTH);
        textLabels.add(cT);subLabels.add(cS);
        sparkBar=new SparkBar(ds.weeklySpend(),new String[]{"W-5","W-4","W-3","W-2","W-1","Now"},tm.acc(),tm.sub());
        double s2=0,c2=0,f2=0;for(Account a:ds.accounts){if("Savings".equals(a.type))s2+=a.balance;else if("Current".equals(a.type))c2+=a.balance;else f2+=a.balance;}
        nwBar=new NetWorthBar(s2,c2,f2,tm);
        JLabel nwT=new JLabel("  Savings  |  Current  |  FD  — portfolio composition");
        nwT.setFont(new Font(F,Font.PLAIN,FL));nwT.setForeground(tm.sub());subLabels.add(nwT);
        JPanel nwP=new JPanel(new BorderLayout(0,3));nwP.setOpaque(false);nwP.add(nwBar,BorderLayout.CENTER);nwP.add(nwT,BorderLayout.SOUTH);
        cc.add(cHdr,BorderLayout.NORTH);cc.add(sparkBar,BorderLayout.CENTER);cc.add(nwP,BorderLayout.SOUTH);
        // Recent activity card
        RoundedPanel rc2=new RoundedPanel(16,tm.card(),tm.bord(),true);rc2.setLayout(new BorderLayout());
        rc2.setBorder(new EmptyBorder(18,20,14,20));allCards.add(rc2);
        JLabel rh=new JLabel("Recent Transactions");rh.setFont(new Font(F,Font.BOLD,FL+3));rh.setForeground(tm.text());
        rh.setBorder(new EmptyBorder(0,0,10,0));textLabels.add(rh);
        String[]rCols={"Date","To/From","Amount","Type"};
        DefaultTableModel rm=new DefaultTableModel(rCols,0){public boolean isCellEditable(int a,int b){return false;}};
        int cnt=0;for(Transaction t:ds.txns){if(cnt++>=7)break;rm.addRow(new Object[]{t.date,t.to,inr(t.amount),t.type});}
        StyledTable rt=new StyledTable(rm,tm);tables.add(rt);
        rt.setDefaultRenderer(Object.class,txRenderer());
        JScrollPane rsp=new JScrollPane(rt);rsp.setBorder(BorderFactory.createLineBorder(tm.bord()));
        rsp.getViewport().setBackground(tm.card());
        rc2.add(rh,BorderLayout.NORTH);rc2.add(rsp,BorderLayout.CENTER);
        mid.add(cc);mid.add(rc2);
        p.add(hRow,BorderLayout.NORTH);p.add(grid,BorderLayout.CENTER);p.add(mid,BorderLayout.SOUTH);return p;
    }
    void refreshDash(){if(statCards[0]==null)return;
        statCards[0].refresh(inr(ds.totalBal()),null,tm);statCards[1].refresh(""+ds.accounts.size(),null,tm);
        statCards[2].refresh(""+ds.recentTx(),null,tm);statCards[3].refresh(inr(ds.monthSpend()),null,tm);
        if(nwBar!=null){double s=0,c=0,f=0;for(Account a:ds.accounts){if("Savings".equals(a.type))s+=a.balance;else if("Current".equals(a.type))c+=a.balance;else f+=a.balance;}
            nwBar.savings=s;nwBar.current=c;nwBar.fd=f;nwBar.repaint();}
        if(sparkBar!=null){sparkBar.values=ds.weeklySpend();sparkBar.repaint();}
    }
    // --- Accounts ---
    JPanel mkAccounts(){
        JPanel p=new JPanel(new BorderLayout(0,0));p.setOpaque(false);p.setBorder(new EmptyBorder(24,26,24,26));
        JLabel h=new JLabel("Accounts");h.setFont(new Font(F,Font.BOLD,FH+4));h.setForeground(tm.text());
        JLabel hs=new JLabel("Manage accounts — select a row to view details or perform actions");
        hs.setFont(new Font(F,Font.PLAIN,FL+1));hs.setForeground(tm.sub());
        JPanel hRow=new JPanel(new BorderLayout(0,3));hRow.setOpaque(false);hRow.setBorder(new EmptyBorder(0,0,14,0));
        hRow.add(h,BorderLayout.NORTH);hRow.add(hs,BorderLayout.SOUTH);
        textLabels.add(h);subLabels.add(hs);
        String[]cols={"Account No.","Holder Name","Type","Balance","City","Status"};
        accMdl=new DefaultTableModel(cols,0){public boolean isCellEditable(int a,int b){return false;}};
        refreshAccMdl();
        accTable=new StyledTable(accMdl,tm);tables.add(accTable);
        accTable.getColumnModel().getColumn(5).setCellRenderer(badgeRenderer(new String[]{"Active"},new Color[]{GREEN},RED));
        accTable.getColumnModel().getColumn(2).setCellRenderer(badgeRenderer(new String[]{"Savings","Current","Fixed Deposit"},new Color[]{BLUE_D,PURPLE,GOLD},TEAL));
        JScrollPane sp=new JScrollPane(accTable);sp.setBorder(BorderFactory.createLineBorder(tm.bord(),1));
        sp.getViewport().setBackground(tm.card());
        // Detail sidebar
        JPanel sidebar=new JPanel(new BorderLayout());sidebar.setOpaque(false);
        sidebar.setBorder(new EmptyBorder(0,16,0,0));sidebar.setPreferredSize(new Dimension(280,0));
        RoundedPanel detCard=new RoundedPanel(16,tm.card(),tm.bord(),true);detCard.setLayout(new BorderLayout(0,0));
        detCard.setBorder(new EmptyBorder(20,20,20,20));allCards.add(detCard);
        JLabel dh=new JLabel("Account Details");dh.setFont(new Font(F,Font.BOLD,FL+3));dh.setForeground(tm.text());
        dh.setBorder(new EmptyBorder(0,0,14,0));textLabels.add(dh);
        detailId=detLbl("—");detailOwner=detLbl("Select a row");detailType=detLbl("—");
        detailBal=detLbl("—");detailStatus=detLbl("—");detailCity=detLbl("—");
        textLabels.add(detailId);textLabels.add(detailOwner);textLabels.add(detailType);
        textLabels.add(detailBal);textLabels.add(detailStatus);textLabels.add(detailCity);
        JPanel fields=new JPanel(new GridLayout(6,1,0,8));fields.setOpaque(false);
        fields.add(mkDetRow("A/C No",detailId));fields.add(mkDetRow("Holder",detailOwner));
        fields.add(mkDetRow("Type",detailType));fields.add(mkDetRow("Balance",detailBal));
        fields.add(mkDetRow("City",detailCity));fields.add(mkDetRow("Status",detailStatus));
        JLabel qh=new JLabel("Quick Transaction");qh.setFont(new Font(F,Font.BOLD,FL+2));qh.setForeground(tm.text());
        qh.setBorder(new EmptyBorder(16,0,8,0));textLabels.add(qh);
        tfQuick=new JTextField();tfQuick.setFont(new Font(F,Font.PLAIN,FB));tfQuick.setToolTipText("Amount in \u20B9");
        eQuick=el();
        JPanel qBtns=new JPanel(new GridLayout(1,2,8,0));qBtns.setOpaque(false);
        PulseButton dep=new PulseButton("+ Deposit",GREEN);dep.setBorder(new EmptyBorder(8,10,8,10));
        PulseButton wdr=new PulseButton("- Withdraw",RED);wdr.setBorder(new EmptyBorder(8,10,8,10));
        dep.addActionListener(e->doQuick(true));wdr.addActionListener(e->doQuick(false));
        qBtns.add(dep);qBtns.add(wdr);
        JPanel qP=new JPanel(new BorderLayout(0,4));qP.setOpaque(false);
        qP.add(qh,BorderLayout.NORTH);qP.add(tfQuick,BorderLayout.CENTER);
        JPanel qBot=new JPanel(new BorderLayout(0,4));qBot.setOpaque(false);
        qBot.add(qBtns,BorderLayout.NORTH);qBot.add(eQuick,BorderLayout.SOUTH);
        qP.add(qBot,BorderLayout.SOUTH);
        JPanel sBtns=new JPanel(new GridLayout(1,2,8,0));sBtns.setOpaque(false);sBtns.setBorder(new EmptyBorder(14,0,0,0));
        PulseButton freeze=new PulseButton("Freeze",ROSE,true);freeze.setForeground(ROSE);freeze.setBorder(new EmptyBorder(7,8,7,8));
        PulseButton activate=new PulseButton("Activate",GREEN,true);activate.setForeground(GREEN);activate.setBorder(new EmptyBorder(7,8,7,8));
        freeze.addActionListener(e->toggleStatus("Frozen"));activate.addActionListener(e->toggleStatus("Active"));
        sBtns.add(freeze);sBtns.add(activate);
        JPanel detBody=new JPanel(new BorderLayout());detBody.setOpaque(false);
        detBody.add(fields,BorderLayout.NORTH);detBody.add(qP,BorderLayout.CENTER);detBody.add(sBtns,BorderLayout.SOUTH);
        detCard.add(dh,BorderLayout.NORTH);detCard.add(detBody,BorderLayout.CENTER);
        sidebar.add(detCard,BorderLayout.CENTER);
        accTable.getSelectionModel().addListSelectionListener(e->{if(!e.getValueIsAdjusting())populateDetail();});
        JPanel content=new JPanel(new BorderLayout());content.setOpaque(false);
        content.add(sp,BorderLayout.CENTER);content.add(sidebar,BorderLayout.EAST);
        p.add(hRow,BorderLayout.NORTH);p.add(content,BorderLayout.CENTER);return p;
    }
    JLabel detLbl(String t){JLabel l=new JLabel(t);l.setFont(new Font(F,Font.PLAIN,FB));l.setForeground(tm.text());return l;}
    JPanel mkDetRow(String lbl,JLabel val){JPanel r=new JPanel(new BorderLayout(4,0));r.setOpaque(false);
        JLabel l=new JLabel(lbl);l.setFont(new Font(F,Font.BOLD,FL));l.setForeground(tm.sub());l.setPreferredSize(new Dimension(58,0));
        subLabels.add(l);r.add(l,BorderLayout.WEST);r.add(val,BorderLayout.CENTER);return r;}
    void populateDetail(){int r=accTable.getSelectedRow();if(r<0)return;
        String id=(String)accMdl.getValueAt(r,0);Account a=ds.find(id);if(a==null)return;
        detailId.setText(a.id);detailOwner.setText(a.owner);detailType.setText(a.type);
        detailBal.setText(inr(a.balance));detailStatus.setText(a.status);detailCity.setText(a.city);}
    void doQuick(boolean dep){int r=accTable.getSelectedRow();if(r<0){setE(eQuick,"Select an account first.");return;}
        eQuick.setText(" ");double amt=0;
        try{amt=Double.parseDouble(tfQuick.getText().trim());if(amt<=0)throw new Exception();}
        catch(Exception e){setE(eQuick,"Enter a valid amount.");return;}
        String id=(String)accMdl.getValueAt(r,0);Account a=ds.find(id);if(a==null)return;
        if(!dep&&a.balance<amt){setE(eQuick,"Insufficient balance.");return;}
        if(!dep&&!"Active".equals(a.status)){setE(eQuick,"Account is not active.");return;}
        try{String dt=DataStore.SDF.format(new Date());
            if(dep){
                a.balance+=amt; ds.api.updateBalance(id, a.balance);
                Transaction t = new Transaction(dt,"CASH-DEPOSIT",id,amt,"Credit","Counter Cash Deposit");
                ds.txns.add(0, t); ds.api.insertTransaction(t);
            } else {
                a.balance-=amt; ds.api.updateBalance(id, a.balance);
                Transaction t = new Transaction(dt,id,"CASH-WITHDRAWAL",amt,"Debit","Counter Cash Withdrawal");
                ds.txns.add(0, t); ds.api.insertTransaction(t);
            }
            refreshAccMdl();refreshDash();populateDetail();tfQuick.setText("");
            String op=dep?"Deposited":"Withdrew";
            status("✓  "+op+" "+inr(amt)+" "+(dep?"into":"from")+" "+id,GREEN);toast(op+": "+inr(amt)+" — "+id,GREEN);
        }catch(Exception e){status("✗  Error: "+e.getMessage(),RED);}}
    void toggleStatus(String s){int r=accTable.getSelectedRow();if(r<0){toast("Select an account.",GOLD);return;}
        String id=(String)accMdl.getValueAt(r,0);Account a=ds.find(id);if(a==null)return;
        a.status=s;refreshAccMdl();populateDetail();status("✓  "+id+" → "+s,GREEN);toast(id+": "+s,GREEN);}
    void refreshAccMdl(){if(accMdl==null)return;accMdl.setRowCount(0);
        for(Account a:ds.accounts)accMdl.addRow(new Object[]{a.id,a.owner,a.type,inr(a.balance),a.city,a.status});}
    // --- Transactions ---
    JPanel mkTransactions(){
        JPanel p=new JPanel(new BorderLayout(0,0));p.setOpaque(false);p.setBorder(new EmptyBorder(24,26,24,26));
        JLabel h=new JLabel("Transactions");h.setFont(new Font(F,Font.BOLD,FH+4));h.setForeground(tm.text());
        JLabel hs=new JLabel("Full transaction history — UPI, NEFT, RTGS, IMPS and more");
        hs.setFont(new Font(F,Font.PLAIN,FL+1));hs.setForeground(tm.sub());
        JPanel hRow=new JPanel(new BorderLayout(0,3));hRow.setOpaque(false);hRow.setBorder(new EmptyBorder(0,0,12,0));
        hRow.add(h,BorderLayout.NORTH);hRow.add(hs,BorderLayout.SOUTH);
        textLabels.add(h);subLabels.add(hs);
        JPanel bar=new JPanel(new FlowLayout(FlowLayout.LEFT,10,0));bar.setOpaque(false);bar.setBorder(new EmptyBorder(0,0,12,0));
        JLabel fl=new JLabel("Account:");fl.setFont(new Font(F,Font.BOLD,FL+1));fl.setForeground(tm.text());subLabels.add(fl);
        txFilt=new JComboBox<>();txFilt.addItem("All Accounts");for(Account a:ds.accounts)txFilt.addItem(a.id+" \u2014 "+a.owner);
        txFilt.setFont(new Font(F,Font.PLAIN,FB));txFilt.setPreferredSize(new Dimension(220,32));
        JLabel sl=new JLabel("Search:");sl.setFont(new Font(F,Font.BOLD,FL+1));sl.setForeground(tm.text());subLabels.add(sl);
        tfSearch=new JTextField(16);tfSearch.setFont(new Font(F,Font.PLAIN,FB));
        bar.add(fl);bar.add(txFilt);bar.add(Box.createHorizontalStrut(12));bar.add(sl);bar.add(tfSearch);
        String[]cols={"Date","From","To","Amount","Mode","Description"};
        txMdl=new DefaultTableModel(cols,0){public boolean isCellEditable(int a,int b){return false;}};
        refreshTxMdl(null,null);
        StyledTable txT=new StyledTable(txMdl,tm);tables.add(txT);
        txT.setDefaultRenderer(Object.class,txRenderer());
        JScrollPane sp=new JScrollPane(txT);sp.setBorder(BorderFactory.createLineBorder(tm.bord(),1));
        sp.getViewport().setBackground(tm.card());
        txFilt.addActionListener(e->filterTx());
        tfSearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener(){
            public void insertUpdate(javax.swing.event.DocumentEvent e){filterTx();}
            public void removeUpdate(javax.swing.event.DocumentEvent e){filterTx();}
            public void changedUpdate(javax.swing.event.DocumentEvent e){filterTx();}});
        JPanel north=new JPanel(new BorderLayout());north.setOpaque(false);
        north.add(hRow,BorderLayout.NORTH);north.add(bar,BorderLayout.SOUTH);
        p.add(north,BorderLayout.NORTH);p.add(sp,BorderLayout.CENTER);return p;
    }
    DefaultTableCellRenderer txRenderer(){
        return new DefaultTableCellRenderer(){
            @Override public Component getTableCellRendererComponent(JTable tb,Object v,boolean sel,boolean foc,int r,int c){
                Component cp=super.getTableCellRendererComponent(tb,v,sel,foc,r,c);
                if(!sel){cp.setBackground(r%2==0?tm.card():tm.row());cp.setForeground(tm.text());}
                if(c==4&&!sel){String s=v==null?"":v.toString();cp.setForeground("Credit".equals(s)?GREEN:"Debit".equals(s)?RED:tm.text());}
                if(c==3&&!sel){if(cp instanceof JLabel){((JLabel)cp).setFont(new Font(F,Font.BOLD,FB));}}
                if(cp instanceof JLabel)((JLabel)cp).setBorder(new EmptyBorder(0,12,0,12));return cp;}};
    }
    TableCellRenderer badgeRenderer(String[]match,Color[]matchColors,Color defaultColor){
        return new DefaultTableCellRenderer(){
            @Override public Component getTableCellRendererComponent(JTable tb,Object v,boolean sel,boolean foc,int r,int c){
                JPanel w=new JPanel(new FlowLayout(FlowLayout.LEFT,6,7));w.setOpaque(false);
                w.setBackground(sel?tm.acc():(r%2==0?tm.card():tm.row()));
                String s=v==null?"":v.toString();Color bg=defaultColor;
                for(int i=0;i<match.length;i++)if(match[i].equals(s))bg=matchColors[i];
                BadgeLabel b=new BadgeLabel("  "+s+"  ",bg);b.setPreferredSize(new Dimension(90,20));w.add(b);return w;}};
    }
    void filterTx(){String s=(String)txFilt.getSelectedItem();String q=tfSearch==null?"":tfSearch.getText().trim().toLowerCase();
        refreshTxMdl(s==null||s.startsWith("All")?null:s.split(" \u2014 ")[0],q.isEmpty()?null:q);}
    void refreshTxMdl(String id,String q){if(txMdl==null)return;txMdl.setRowCount(0);
        for(Transaction t:ds.txns){boolean ma=id==null||t.from.equals(id)||t.to.equals(id);
            boolean mq=q==null||t.desc.toLowerCase().contains(q)||t.from.toLowerCase().contains(q)||t.to.toLowerCase().contains(q);
            if(ma&&mq)txMdl.addRow(new Object[]{t.date,t.from,t.to,inr(t.amount),t.type,t.desc});}}
    // --- Transfer ---
    JPanel mkTransfer(){
        JPanel outer=new JPanel(new GridBagLayout());outer.setOpaque(false);
        RoundedPanel card=new RoundedPanel(20,tm.card(),tm.bord(),true);card.setLayout(new GridBagLayout());
        card.setBorder(new EmptyBorder(32,42,32,42));card.setPreferredSize(new Dimension(540,550));allCards.add(card);
        GridBagConstraints g=gbc();
        JLabel h=new JLabel("Fund Transfer");h.setFont(new Font(F,Font.BOLD,FH+4));h.setForeground(tm.text());
        JLabel hs=new JLabel("NEFT / RTGS / IMPS / UPI — instant settlement");
        hs.setFont(new Font(F,Font.PLAIN,FL+1));hs.setForeground(tm.sub());
        JPanel tRow=new JPanel(new BorderLayout(0,4));tRow.setOpaque(false);tRow.add(h,BorderLayout.NORTH);tRow.add(hs,BorderLayout.SOUTH);
        textLabels.add(h);subLabels.add(hs);
        g.gridy=0;g.insets=new Insets(0,0,22,0);card.add(tRow,g);
        srcCmb=cmb();dstCmb=cmb();for(Account a:ds.accounts){srcCmb.addItem(a.id+" \u2014 "+a.owner);dstCmb.addItem(a.id+" \u2014 "+a.owner);}
        tfAmount=tf();tfDesc=tf();eSrc=el();eDst=el();eAmount=el();g.insets=new Insets(4,0,0,0);
        addF(card,g,1,"Remitter Account",srcCmb,eSrc);addF(card,g,5,"Beneficiary Account",dstCmb,eDst);
        addF(card,g,9,"Amount (\u20B9)",tfAmount,eAmount);addF(card,g,13,"Remarks / Narration",tfDesc,null);
        btnXfer=new PulseButton("  Submit Transfer  ",tm.acc());btnXfer.base=tm.acc();btnXfer.hov=BLUE_H;
        g.gridy=17;g.insets=new Insets(26,0,0,0);card.add(btnXfer,g);
        btnXfer.addActionListener(e->doXfer());outer.add(card);return outer;
    }
    void doXfer(){clearE(eSrc,eDst,eAmount);boolean ok=true;
        String si=((String)srcCmb.getSelectedItem()).split(" \u2014 ")[0];
        String di=((String)dstCmb.getSelectedItem()).split(" \u2014 ")[0];
        if(si.equals(di)){setE(eSrc,"Remitter and beneficiary must differ.");ok=false;}
        double amt=0;
        try{amt=Double.parseDouble(tfAmount.getText().trim());if(amt<=0)throw new Exception();}
        catch(Exception ex){setE(eAmount,"Enter a valid positive amount.");ok=false;}
        if(!ok)return;
        Account src=ds.find(si),dst=ds.find(di);
        if(src==null||dst==null){setE(eSrc,"Invalid account.");return;}
        if(!"Active".equals(src.status)){setE(eSrc,"Account "+si+" is not active.");return;}
        if(src.balance<amt){setE(eAmount,"Insufficient funds. Available: "+inr(src.balance));return;}
        final double fa=amt;final Account fs=src,fd=dst;final String fsi=si,fdi=di;
        btnXfer.setEnabled(false);btnXfer.setText("  Processing\u2026  ");
        new javax.swing.Timer(520,e->{
            try{fs.balance-=fa; ds.api.updateBalance(fsi, fs.balance);
                fd.balance+=fa; ds.api.updateBalance(fdi, fd.balance);
                String d=tfDesc.getText().trim().isEmpty()?"IMPS Transfer":tfDesc.getText().trim();
                String dt=DataStore.SDF.format(new Date());
                Transaction t1 = new Transaction(dt,fsi,fdi,fa,"Credit",d);
                Transaction t2 = new Transaction(dt,fsi,fdi,fa,"Debit",d);
                ds.txns.add(0, t1); ds.api.insertTransaction(t1);
                ds.txns.add(1, t2); ds.api.insertTransaction(t2);
                refreshAccMdl();refreshTxMdl(null,null);refreshDash();syncCombos();tfAmount.setText("");tfDesc.setText("");
                status("✓  Transfer of "+inr(fa)+" from "+fsi+" \u2192 "+fdi+" completed.",GREEN);
                toast(inr(fa)+" credited to "+fdi,GREEN);
            }catch(Exception ex){status("✗  Error: "+ex.getMessage(),RED);toast("Transfer failed",RED);}
            btnXfer.setText("  Submit Transfer  ");btnXfer.setEnabled(true);
            ((javax.swing.Timer)e.getSource()).stop();}).start();
    }
    // --- New Account ---
    JPanel mkNewAccount(){
        JPanel outer=new JPanel(new GridBagLayout());outer.setOpaque(false);
        RoundedPanel card=new RoundedPanel(20,tm.card(),tm.bord(),true);card.setLayout(new GridBagLayout());
        card.setBorder(new EmptyBorder(32,42,32,42));card.setPreferredSize(new Dimension(500,440));allCards.add(card);
        GridBagConstraints g=gbc();
        JLabel h=new JLabel("Open New Account");h.setFont(new Font(F,Font.BOLD,FH+4));h.setForeground(tm.text());
        JLabel hs=new JLabel("KYC verified — Aadhaar & PAN linked account opening");
        hs.setFont(new Font(F,Font.PLAIN,FL+1));hs.setForeground(tm.sub());
        JPanel tRow=new JPanel(new BorderLayout(0,4));tRow.setOpaque(false);tRow.add(h,BorderLayout.NORTH);tRow.add(hs,BorderLayout.SOUTH);
        textLabels.add(h);subLabels.add(hs);
        g.gridy=0;g.insets=new Insets(0,0,22,0);card.add(tRow,g);
        tfOwner=tf();tfDeposit=tf();
        cbType=new JComboBox<>(new String[]{"Savings","Current","Fixed Deposit"});cbType.setFont(new Font(F,Font.PLAIN,FB));
        eOwner=el();eDeposit=el();g.insets=new Insets(4,0,0,0);
        addF(card,g,1,"Account Holder Name",tfOwner,eOwner);addF(card,g,5,"Account Type",cbType,null);
        addF(card,g,9,"Initial Deposit (\u20B9)",tfDeposit,eDeposit);
        btnCreate=new PulseButton("  Open Account  ",tm.acc());btnCreate.base=tm.acc();btnCreate.hov=BLUE_H;
        g.gridy=13;g.insets=new Insets(26,0,0,0);card.add(btnCreate,g);
        btnCreate.addActionListener(e->doCreate());outer.add(card);return outer;
    }
    void doCreate(){clearE(eOwner,eDeposit);boolean ok=true;
        String own=tfOwner.getText().trim();
        if(own.isEmpty()){setE(eOwner,"Account holder name is required.");ok=false;}
        double dep=0;
        try{dep=Double.parseDouble(tfDeposit.getText().trim());if(dep<0)throw new Exception();}
        catch(Exception ex){setE(eDeposit,"Enter a valid deposit amount (\u2265 0).");ok=false;}
        if(!ok)return;
        try{String id=ds.nextId();String tp=(String)cbType.getSelectedItem();
            Account newAcc = new Account(id,own,tp,dep,"India");
            ds.accounts.add(newAcc); ds.api.insertAccount(newAcc);
            refreshAccMdl();refreshDash();syncCombos();tfOwner.setText("");tfDeposit.setText("");
            status("✓  Account "+id+" opened for "+own+" | Initial deposit: "+inr(dep),GREEN);
            toast("Account "+id+" created successfully!",GREEN);
        }catch(Exception ex){status("\u2717  Error: "+ex.getMessage(),RED);}
    }
    // --- Reports ---
    JPanel mkReports(){
        JPanel p=new JPanel(new BorderLayout(0,18));p.setOpaque(false);p.setBorder(new EmptyBorder(24,26,24,26));
        JLabel h=new JLabel("Reports & Analytics");h.setFont(new Font(F,Font.BOLD,FH+4));h.setForeground(tm.text());
        JLabel hs=new JLabel("Financial summaries, portfolio analysis and data export");
        hs.setFont(new Font(F,Font.PLAIN,FL+1));hs.setForeground(tm.sub());
        JPanel hRow=new JPanel(new BorderLayout(0,3));hRow.setOpaque(false);hRow.setBorder(new EmptyBorder(0,0,4,0));
        hRow.add(h,BorderLayout.NORTH);hRow.add(hs,BorderLayout.SOUTH);
        textLabels.add(h);subLabels.add(hs);
        JPanel sumRow=new JPanel(new GridLayout(1,3,16,0));sumRow.setOpaque(false);
        sumRow.add(mkSummaryCard("Total Income (30d)",inr(ds.monthIncome()),GREEN,"📈"));
        sumRow.add(mkSummaryCard("Total Expenses (30d)",inr(ds.monthSpend()),RED,"📉"));
        double net=ds.monthIncome()-ds.monthSpend();
        sumRow.add(mkSummaryCard("Net Cash Flow",inr(net),net>=0?GREEN:RED,net>=0?"✅":"⚠️"));
        JPanel mid=new JPanel(new GridLayout(1,2,18,0));mid.setOpaque(false);
        RoundedPanel breakdown=new RoundedPanel(16,tm.card(),tm.bord(),true);breakdown.setLayout(new BorderLayout());
        breakdown.setBorder(new EmptyBorder(18,20,16,20));allCards.add(breakdown);
        JLabel bh=new JLabel("Account Portfolio");bh.setFont(new Font(F,Font.BOLD,FL+3));bh.setForeground(tm.text());
        bh.setBorder(new EmptyBorder(0,0,10,0));textLabels.add(bh);
        String[]bCols={"A/C No","Holder","Balance","Share %","Type"};
        DefaultTableModel bMdl=new DefaultTableModel(bCols,0){public boolean isCellEditable(int a,int b){return false;}};
        double tot=ds.totalBal();
        for(Account a:ds.accounts){double pct=tot>0?a.balance/tot*100:0;
            bMdl.addRow(new Object[]{a.id,a.owner,inr(a.balance),String.format("%.1f%%",pct),a.type});}
        StyledTable bTbl=new StyledTable(bMdl,tm);tables.add(bTbl);
        JScrollPane bSp=new JScrollPane(bTbl);bSp.setBorder(BorderFactory.createLineBorder(tm.bord()));
        bSp.getViewport().setBackground(tm.card());
        breakdown.add(bh,BorderLayout.NORTH);breakdown.add(bSp,BorderLayout.CENTER);
        RoundedPanel txBreak=new RoundedPanel(16,tm.card(),tm.bord(),true);txBreak.setLayout(new BorderLayout());
        txBreak.setBorder(new EmptyBorder(18,20,16,20));allCards.add(txBreak);
        JLabel th=new JLabel("Transaction Analysis");th.setFont(new Font(F,Font.BOLD,FL+3));th.setForeground(tm.text());
        th.setBorder(new EmptyBorder(0,0,10,0));textLabels.add(th);
        String[]tCols={"Category","Count","Total Amount","Avg per Txn"};
        DefaultTableModel tMdl=new DefaultTableModel(tCols,0){public boolean isCellEditable(int a,int b){return false;}};
        double[]cr={0,0},db={0,0};
        for(Transaction t:ds.txns){if("Credit".equals(t.type)){cr[0]++;cr[1]+=t.amount;}else{db[0]++;db[1]+=t.amount;}}
        tMdl.addRow(new Object[]{"Credit",(int)cr[0],inr(cr[1]),cr[0]>0?inr(cr[1]/cr[0]):"—"});
        tMdl.addRow(new Object[]{"Debit",(int)db[0],inr(db[1]),db[0]>0?inr(db[1]/db[0]):"—"});
        for(Account a:ds.accounts){List<Transaction>at=ds.forAccount(a.id);double tot2=0;
            for(Transaction t:at)tot2+=t.amount;
            tMdl.addRow(new Object[]{a.owner.split(" ")[0]+" ("+a.id+")",at.size(),inr(tot2),at.isEmpty()?"—":inr(tot2/at.size())});}
        StyledTable tTbl=new StyledTable(tMdl,tm);tables.add(tTbl);
        JScrollPane tSp=new JScrollPane(tTbl);tSp.setBorder(BorderFactory.createLineBorder(tm.bord()));
        tSp.getViewport().setBackground(tm.card());
        txBreak.add(th,BorderLayout.NORTH);txBreak.add(tSp,BorderLayout.CENTER);
        mid.add(breakdown);mid.add(txBreak);
        JPanel expRow=new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));expRow.setOpaque(false);
        PulseButton csvBtn=new PulseButton("  \u2B07  Export CSV  ",PURPLE);csvBtn.base=PURPLE;csvBtn.hov=PURPLE.darker();
        csvBtn.addActionListener(e->exportCSV());
        PulseButton refBtn=new PulseButton("  \u21BA  Refresh  ",TEAL,true);refBtn.setForeground(TEAL);
        refBtn.addActionListener(e->{bMdl.setRowCount(0);double t2=ds.totalBal();
            for(Account a:ds.accounts){double pc=t2>0?a.balance/t2*100:0;
                bMdl.addRow(new Object[]{a.id,a.owner,inr(a.balance),String.format("%.1f%%",pc),a.type});}
            status("✓  Reports refreshed.",GREEN);});
        expRow.add(csvBtn);expRow.add(Box.createHorizontalStrut(12));expRow.add(refBtn);
        JPanel body=new JPanel(new BorderLayout(0,16));body.setOpaque(false);
        body.add(sumRow,BorderLayout.NORTH);body.add(mid,BorderLayout.CENTER);body.add(expRow,BorderLayout.SOUTH);
        p.add(hRow,BorderLayout.NORTH);p.add(body,BorderLayout.CENTER);return p;
    }
    RoundedPanel mkSummaryCard(String title,String value,Color accent,String icon){
        RoundedPanel card=new RoundedPanel(16,tm.card(),tm.bord(),true);card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(18,20,18,20));allCards.add(card);
        JLabel ttl=new JLabel(title);ttl.setFont(new Font(F,Font.BOLD,FL+1));ttl.setForeground(tm.sub());subLabels.add(ttl);
        JLabel val=new JLabel(value);val.setFont(new Font(F,Font.BOLD,22));val.setForeground(accent);
        JLabel ico=new JLabel(icon);ico.setFont(new Font("Dialog",Font.PLAIN,22));
        JPanel top=new JPanel(new BorderLayout());top.setOpaque(false);top.add(ttl,BorderLayout.CENTER);top.add(ico,BorderLayout.EAST);
        card.add(top,BorderLayout.NORTH);card.add(val,BorderLayout.CENTER);return card;
    }
    void exportCSV(){JFileChooser fc=new JFileChooser();fc.setSelectedFile(new java.io.File("nexusbank_transactions.csv"));
        if(fc.showSaveDialog(frame)!=JFileChooser.APPROVE_OPTION)return;
        try(java.io.PrintWriter pw=new java.io.PrintWriter(fc.getSelectedFile())){
            pw.println("Date,From,To,Amount,Type,Description");
            for(Transaction t:ds.txns)pw.printf("%s,%s,%s,%.2f,%s,\"%s\"%n",t.date,t.from,t.to,t.amount,t.type,t.desc);
            status("✓  Exported "+ds.txns.size()+" transactions to "+fc.getSelectedFile().getName(),GREEN);
            toast("CSV exported successfully",GREEN);
        }catch(Exception ex){status("\u2717  Export failed: "+ex.getMessage(),RED);}}
    // --- Theme Application ---
    void applyTheme(){
        Color bg=tm.bg(),card=tm.card(),text=tm.text(),bord=tm.bord();
        rootPanel.setBackground(bg);tabs.setBackground(bg);tabs.setForeground(text);
        // Update tracked label lists
        for(JLabel l:textLabels)l.setForeground(text);
        for(JLabel l:subLabels)l.setForeground(tm.sub());
        // Update cards
        for(RoundedPanel c:allCards){c.bg=card;c.bord=bord;c.repaint();}
        // Update stat cards
        for(StatCard sc:statCards)if(sc!=null)sc.applyColors(tm);
        // Update tables
        for(StyledTable t:tables)t.theme();
        // Update buttons
        if(btnXfer!=null){btnXfer.base=tm.acc();btnXfer.hov=BLUE_H;}
        if(btnCreate!=null){btnCreate.base=tm.acc();btnCreate.hov=BLUE_H;}
        // Update spark/nw
        if(sparkBar!=null){sparkBar.barColor=tm.acc();sparkBar.labelColor=tm.sub();sparkBar.repaint();}
        if(nwBar!=null){nwBar.tm=tm;nwBar.repaint();}
        // Deep sweep for everything else
        deepSweep(rootPanel,bg,text,bord);
        frame.repaint();
    }
    void deepSweep(Container c,Color bg,Color text,Color bord){
        for(Component comp:c.getComponents()){
            // Always recurse into containers first
            if(comp instanceof Container)deepSweep((Container)comp,bg,text,bord);
            // Now apply colors — skip custom-painted components that manage their own bg
            if(comp instanceof StatCard||comp instanceof SparkBar||comp instanceof NetWorthBar
                ||comp instanceof GradientPanel||comp instanceof BadgeLabel||comp instanceof PulseButton)continue;
            if(comp instanceof RoundedPanel)continue; // bg handled via allCards list above
            if(comp instanceof StyledTable)continue;  // handled via tables list
            if(comp instanceof JLabel){comp.setForeground(text);continue;}
            if(comp instanceof JComboBox){comp.setBackground(tm.card());comp.setForeground(text);continue;}
            if(comp instanceof JTextField){
                comp.setBackground(tm.card());comp.setForeground(text);
                ((JTextField)comp).setCaretColor(text);
                ((JTextField)comp).setBorder(new CompoundBorder(new LineBorder(bord,1,true),new EmptyBorder(7,10,7,10)));continue;}
            if(comp instanceof JScrollPane){
                ((JScrollPane)comp).getViewport().setBackground(tm.card());comp.setBackground(bg);continue;}
            if(comp instanceof JTabbedPane){comp.setBackground(bg);comp.setForeground(text);continue;}
            // Generic panels and other components
            comp.setBackground(bg);comp.setForeground(text);
        }
    }
    // --- Helpers ---
    GridBagConstraints gbc(){GridBagConstraints g=new GridBagConstraints();g.fill=GridBagConstraints.HORIZONTAL;g.gridx=0;g.weightx=1;g.insets=new Insets(6,0,2,0);return g;}
    JTextField tf(){JTextField t=new JTextField();t.setFont(new Font(F,Font.PLAIN,FB));t.setBackground(tm.card());t.setForeground(tm.text());t.setCaretColor(tm.text());t.setBorder(new CompoundBorder(new LineBorder(tm.bord(),1,true),new EmptyBorder(7,10,7,10)));return t;}
    JComboBox<String> cmb(){JComboBox<String>c=new JComboBox<>();c.setFont(new Font(F,Font.PLAIN,FB));c.setBackground(tm.card());c.setForeground(tm.text());return c;}
    JLabel el(){JLabel l=new JLabel(" ");l.setFont(new Font(F,Font.PLAIN,FL));l.setForeground(RED);return l;}
    void setE(JLabel l,String m){if(l!=null)l.setText("\u26A0  "+m);}
    void clearE(JLabel...ls){for(JLabel l:ls)if(l!=null)l.setText(" ");}
    void addF(JPanel p,GridBagConstraints g,int row,String lbl,JComponent f,JLabel e){
        JLabel l=new JLabel(lbl);l.setFont(new Font(F,Font.BOLD,FL+1));l.setForeground(tm.sub());
        l.setBorder(new EmptyBorder(10,0,3,0));subLabels.add(l);
        g.gridy=row;p.add(l,g);g.gridy=row+1;p.add(f,g);if(e!=null){g.gridy=row+2;p.add(e,g);}
    }
    void status(String m,Color c){if(statusBar!=null){statusBar.setText(m);statusBar.setForeground(c);statusDot.setForeground(c);}}
    void toast(String m,Color c){try{new Toast(frame,m,c);}catch(Exception ig){}}
    String inr(double v){
        if(v>=10000000)return String.format("\u20B9%.2fCr",v/10000000);
        if(v>=100000)return String.format("\u20B9%.2fL",v/100000);
        return String.format("\u20B9%,.2f",v);}
    void syncCombos(){if(srcCmb==null)return;srcCmb.removeAllItems();dstCmb.removeAllItems();txFilt.removeAllItems();
        txFilt.addItem("All Accounts");
        for(Account a:ds.accounts){srcCmb.addItem(a.id+" \u2014 "+a.owner);dstCmb.addItem(a.id+" \u2014 "+a.owner);txFilt.addItem(a.id+" \u2014 "+a.owner);}}
    public static void main(String[]args){
        SwingUtilities.invokeLater(()->{NexusBank app=new NexusBank();app.ds.seed();app.build();});
    }
}
