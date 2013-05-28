package platform.server.logics;

import org.antlr.runtime.RecognitionException;
import platform.server.classes.AbstractCustomClass;
import platform.server.classes.ConcreteCustomClass;
import platform.server.logics.linear.LAP;
import platform.server.logics.linear.LCP;
import platform.server.logics.property.CurrentComputerFormulaProperty;
import platform.server.logics.property.CurrentUserFormulaProperty;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.actions.GenerateLoginPasswordActionProperty;
import platform.server.logics.scripted.ScriptingLogicsModule;

import java.io.IOException;

import static platform.server.logics.ServerResourceBundle.getString;


public class AuthenticationLogicsModule extends ScriptingLogicsModule{
    BusinessLogics BL;

    public ConcreteCustomClass computer;
    public AbstractCustomClass user;
    public ConcreteCustomClass systemUser;
    public ConcreteCustomClass customUser;

    public LCP loginCustomUser;
    public LCP customUserLogin;
    public LCP passwordCustomUser;
    public LCP sha256PasswordCustomUser;
    public LCP calculatedHash;
    public LCP currentUser;
    public LCP currentUserName;

    public LCP hostnameComputer;
    public LCP currentComputer;
    public LCP hostnameCurrentComputer;

    public LCP useLDAP;
    public LCP serverLDAP;
    public LCP portLDAP;

    public LAP generateLoginPassword;

    public AuthenticationLogicsModule(BusinessLogics BL, BaseLogicsModule baseLM) throws IOException {
        super(AuthenticationLogicsModule.class.getResourceAsStream("/scripts/system/Authentication.lsf"), baseLM, BL);
        this.BL = BL;
        setBaseLogicsModule(baseLM);
    }

    @Override
    public void initClasses() throws RecognitionException {
        super.initClasses();

        computer = (ConcreteCustomClass) getClassByName("Computer");
        user = (AbstractCustomClass) getClassByName("User");
        systemUser = (ConcreteCustomClass) getClassByName("SystemUser");
        customUser = (ConcreteCustomClass) getClassByName("CustomUser");
    }

    @Override
    public void initProperties() throws RecognitionException {
        // Текущий пользователь
        currentUser = addProperty(null, new LCP<PropertyInterface>(new CurrentUserFormulaProperty("currentUser", user)));
        currentUserName = addJProp("currentUserName", getString("logics.user.current.user.name"), BL.contactLM.nameContact, currentUser);

        super.initProperties();

        // Компьютер
        hostnameComputer = getLCPByName("hostnameComputer");
        currentComputer = addProperty(null, new LCP<PropertyInterface>(new CurrentComputerFormulaProperty("currentComputer", computer)));
        hostnameCurrentComputer = addJProp("hostnameCurrentComputer", getString("logics.current.computer.hostname"), hostnameComputer, currentComputer);

        loginCustomUser = getLCPByName("loginCustomUser");
        customUserLogin = getLCPByName("customUserLogin");

        passwordCustomUser = getLCPByName("passwordCustomUser");
        passwordCustomUser.setEchoSymbols(true);

        sha256PasswordCustomUser = getLCPByName("sha256PasswordCustomUser");
        sha256PasswordCustomUser.setEchoSymbols(true);

        calculatedHash = getLCPByName("calculatedHash");

        useLDAP = getLCPByName("useLDAP");
        serverLDAP = getLCPByName("serverLDAP");
        portLDAP =  getLCPByName("portLDAP");

        generateLoginPassword = addAProp(new GenerateLoginPasswordActionProperty(BL.contactLM.emailContact, loginCustomUser, passwordCustomUser, customUser));


    }
}
