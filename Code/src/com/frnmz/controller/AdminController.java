package com.frnmz.controller;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.frnmz.dao.GenericDAO;
import com.frnmz.dao.RecordDAO;
import com.frnmz.dao.UserDAO;
import com.frnmz.model.Notification;
import com.frnmz.model.Record;
import com.frnmz.model.UserInfo;
import com.frnmz.services.Mailer;
import com.frnmz.utils.Utils;

@SuppressWarnings("all")
@Controller
public class AdminController {
	@Autowired
	private UserDAO userDao;

	@Autowired
	private RecordDAO recordDAO;

	@Autowired
	private GenericDAO genericDAO;

	@Autowired
	private Mailer mailer;

	private ModelAndView mav;
	public AdminController(){
		mav = new ModelAndView();
	}

	private UserInfo initUserInfo(HttpServletRequest request){
		UserInfo userInfo;

		if(null == request.getSession().getAttribute("userInfo")){
			String emailId = SecurityContextHolder.getContext().getAuthentication().getName();
			userInfo = userDao.getUserInfoByEmail(emailId);

			request.getSession().setAttribute("emailId", emailId);
			request.getSession().setAttribute("fullName", userInfo.getFullName());
			request.getSession().setAttribute("userInfo", userInfo);
		} else {
			userInfo = (UserInfo) request.getSession().getAttribute("userInfo");
		}

		return userInfo;
	}

	@RequestMapping(value="/admin/toCreateProfile.htm")
	public ModelAndView toSignUp(HttpServletRequest request){
		initUserInfo(request);
		request.setAttribute("mode", "SIGN_UP");
		mav.addObject("msg", "");
		mav.setViewName("addEditUser");
		return mav;
	}

	@RequestMapping(value="/admin/onSaveProfile.htm")
	public ModelAndView onSaveUser(HttpServletRequest request){
		initUserInfo(request);
		UserInfo userInfo = new UserInfo();
		String mode = request.getParameter("mode");
		String editMode = request.getParameter("editMode");
		
		if("SIGN_UP".equals(mode)){
			userInfo.setEnabled(false);
			userInfo.setAdminAccess(false);
			userInfo.setEmailId(request.getParameter("emailId"));
			mav.setViewName("redirect:/admin/onManageUsers.htm");
		} else {
			if(null != editMode && "MM".equals(editMode)){
				userInfo = userDao.getUserInfoByEmail(request.getParameter("emailId"));
				mav.setViewName("redirect:/admin/onManageUsers.htm");
			} else {
				userInfo = initUserInfo(request);
				mav.setViewName("redirect:/admin/onLoginSuccess.htm");
			}
		}

		userInfo.setFullName(request.getParameter("fullName"));
		userInfo.setMobileNumber(request.getParameter("mobileNumber"));

		String statusMsg = "";
		boolean isDone = genericDAO.updateObject(userInfo);

		if(isDone){
			request.setAttribute("msgType", "Success");

			if(mode.equals("EDIT_PROFILE")){
				statusMsg = "FRNMZ Profile updated successfully.";
				if(null != editMode && !"MM".equals(editMode)){
					request.getSession().setAttribute("userInfo", userInfo);
				}
			}
		} else {
			request.setAttribute("msgType", "Error");

			if(mode.equals("EDIT_PROFILE")){
				statusMsg = "Error while updating FRNMZ Profile.";
			}else{
				statusMsg = "FRNMZ Profile already exist.";
			}
		}

		request.setAttribute("msg", statusMsg);

		return mav;
	}

	@RequestMapping(value="/admin/onLoginSuccess.htm")
	public ModelAndView onLoginSuccess(HttpServletRequest request){
		UserInfo userInfo = initUserInfo(request);

		int balance = 0;
		List<UserInfo> allUsers = userDao.getAllActiveUsers();
		List<UserInfo> defaulters = userDao.getAllWhoHaveNotPaid(userInfo.getEmailId());
		List<Record> allRecords = recordDAO.getAllRecord();

		if(CollectionUtils.isNotEmpty(allRecords)){
			Collections.sort(allRecords);
			balance = allRecords.get(0).getBalance();
			Collections.reverse(allRecords);
		}

		mav.addObject("balance",balance);
		mav.addObject("recordList",allRecords);
		mav.addObject("userList", allUsers);
		mav.addObject("defaulters", defaulters);
		mav.setViewName("notifications");;
		mav.addObject("emailId", userInfo.getEmailId());

		mav.setViewName("welcome");
		return mav;
	}

	@RequestMapping(value="/admin/onChangePassword.htm")
	public ModelAndView changePassword(HttpServletRequest request){
		UserInfo userInfo = initUserInfo(request);

		String oldPassword = request.getParameter("password");
		String newpassword = request.getParameter("newpassword");

		request.setAttribute("msgType", "Error");
		request.setAttribute("mode", "CHANGE_PASSWORD");

		if(oldPassword != null && userInfo != null && oldPassword.equals(userInfo.getPassword())){
			userInfo.setPassword(newpassword);
			if(genericDAO.updateObject(userInfo)){
				request.setAttribute("msgType", "Success");
				request.setAttribute("msg", "Password updated successfully");
			}else{
				request.setAttribute("msg", "Unable to update. Please try again");
			}
		}else{
			request.setAttribute("msg", "Invalid current password");
		}

		mav.setViewName("forgotChangePassword");
		return mav;
	}

	@RequestMapping(value="/admin/toChangePassword.htm")
	public ModelAndView toChangePassword(HttpServletRequest request){
		initUserInfo(request);
		request.setAttribute("mode", "CHANGE_PASSWORD");
		mav.addObject("msg", "");
		mav.setViewName("forgotChangePassword");
		return mav;
	}

	@RequestMapping(value="/admin/toEditProfile.htm")
	public ModelAndView toEditProfile(HttpServletRequest request){
		UserInfo userInfo = initUserInfo(request);
		request.setAttribute("mode", "EDIT_PROFILE");
		request.setAttribute("USER_INFO", userInfo);
		mav.addObject("msg", "");
		mav.setViewName("addEditUser");
		return mav;
	}

	@RequestMapping(value="/admin/toCreditDebit.htm")
	public ModelAndView toCreditDebit(HttpServletRequest request){
		UserInfo userInfo = initUserInfo(request);
		int balance = 0;
		List<Record> allRecords = recordDAO.getAllRecord(userInfo.getEmailId());

		if(CollectionUtils.isNotEmpty(allRecords)){
			Collections.sort(allRecords);
			balance = allRecords.get(0).getBalance();
		}
		if("CREDIT".equals(request.getParameter("action"))){
			mav.addObject("userList", userDao.getAllActiveUsers());
		}

		mav.addObject("balance", balance);
		mav.addObject("msg", "");
		mav.setViewName("amountUpdate");
		return mav;
	}

	@RequestMapping(value="/admin/onAddMemberContribution.htm")
	public ModelAndView onAddMemberContribution(HttpServletRequest request){
		UserInfo userInfo = initUserInfo(request);
		int balance = Integer.parseInt(request.getParameter("balance"));
		int amount = Integer.parseInt(request.getParameter("amount"));
		String[] nameEmail = request.getParameter("member").split("\\|");
		Record record = new Record();
		record.setAdminEmail(userInfo.getEmailId());
		int dt = Integer.parseInt(request.getParameter("date"));
		Date transactionDate = new Date();
		transactionDate.setDate(transactionDate.getDate() - dt);
		record.setTransactionDate(transactionDate);
		record.setTransactionAmount(amount);
		record.setBalance(amount+balance);
		record.setCreditDebit("CREDIT");
		String member = nameEmail[0];
		record.setMemberName(nameEmail[0]);
		if(nameEmail.length == 2){
			record.setMemberEmail(nameEmail[1]);
			member = member.concat(" (").concat(nameEmail[1]).concat(")");
		}
		
		boolean success = genericDAO.insertObject(record);

		if(success){
			List<Record> recordList = recordDAO.getAllRecord(userInfo.getEmailId());
			List<UserInfo> userList = userDao.getAllActiveUsers();
			Collections.sort(recordList);
			balance = recordList.get(0).getBalance();
			Collections.reverse(recordList);
			String status = "Friday Namaz Account Update";
			
			String htmlMessage = Utils.getAccountUpdateMessage(status , amount, balance, member, "By ", recordList);
			boolean mailSuccess = mailer.sendEmail(userList, "", "", "", status, htmlMessage, null, "");

			if(mailSuccess){
				mav.addObject("msgType", "Success");
				mav.addObject("msg", "Member Contribution successfully added");
			} else {
				mav.addObject("msgType", "Success");
				mav.addObject("msg", "Member Contribution successfully added but Mails not sent");
			}
		} else {
			mav.addObject("msgType", "Error");
			mav.addObject("msg", "Error while adding Member Contribution");
		}

		mav.setViewName("redirect:/admin/onLoginSuccess.htm");
		return mav;
	}

	@RequestMapping(value="/admin/onUpdateCabPayment.htm")
	public ModelAndView onUpdateCabPayment(HttpServletRequest request){
		UserInfo userInfo = initUserInfo(request);
		int prevBalance = Integer.parseInt(request.getParameter("balance"));
		int amount = Integer.parseInt(request.getParameter("amount"));
		Record record = new Record();
		record.setAdminEmail(userInfo.getEmailId());
		int dt = Integer.parseInt(request.getParameter("date"));
		Date transactionDate = new Date();
		transactionDate.setDate(transactionDate.getDate() - dt);
		record.setTransactionDate(transactionDate);
		record.setTransactionAmount(amount);
		record.setBalance(prevBalance - amount);
		record.setMemberName("CAB");
		record.setCreditDebit("DEBIT");

		boolean success = genericDAO.insertObject(record);

		if(success){
			List<Record> recordList = recordDAO.getAllRecord(userInfo.getEmailId());
			List<UserInfo> userList = userDao.getAllActiveUsers();
			Collections.sort(recordList);
			int balance = recordList.get(0).getBalance();
			Collections.reverse(recordList);
			String status = "Friday Namaz Account Update";
			String htmlMessage = Utils.getAccountUpdateMessage(status , amount, balance, "CAB", "To ", recordList);
			boolean mailSuccess = mailer.sendEmail(userList, "", "", "", status, htmlMessage, null, "");

			if(mailSuccess){
				mav.addObject("msgType", "Success");
				mav.addObject("msg", "Payment details successfully updated");
			} else {
				mav.addObject("msgType", "Success");
				mav.addObject("msg", "Payment details successfully updated but Mails not sent");
			}
		} else {
			mav.addObject("msgType", "Error");
			mav.addObject("msg", "Error while updating Payment details");
		}

		mav.setViewName("redirect:/admin/onLoginSuccess.htm");
		return mav;
	}

	@RequestMapping(value="/admin/onSendNotification.htm")
	public ModelAndView onSendNotification(HttpServletRequest request){
		UserInfo userInfo = initUserInfo(request);
		List<UserInfo> userList;
		if(null != request.getParameter("toAll") && "on".equals(request.getParameter("toAll"))){
			userList = userDao.getAllUsers();
		} else if(null != request.getParameter("toOnlyDefaulter") && "on".equals(request.getParameter("toOnlyDefaulter"))) {
			userList = userDao.getAllWhoHaveNotPaid(userInfo.getEmailId());
		} else {
			userList = userDao.getAllActiveUsers();
		}

		String htmlMessage;
		String subject;
		boolean isSuccess = false;
		if(null == request.getParameter("voxLink") && null == request.getParameter("ttg") && null == request.getParameter("tol")){
			subject = "Friday Namaz | Request for Contribution";
			List<Record> recordList = recordDAO.getAllRecord(userInfo.getEmailId());
			Collections.sort(recordList);
			int balance = recordList.get(0).getBalance();
			Collections.reverse(recordList);
			htmlMessage = Utils.getContributionRequestMessage(balance, recordList, userInfo.getFullName(), userInfo.getMobileNumber(), request.getParameter("accNo"), request.getParameter("city"));
			isSuccess = mailer.sendEmail(userList, "", "", userInfo.getEmailId(), subject, htmlMessage, null, "");
		} else {
			isSuccess = true;
			subject = "Friday Namaz | "+new SimpleDateFormat("dd MMM").format(new Date());
			String ttg = request.getParameter("ttg");
			String tol = request.getParameter("tol");
			String body = Utils.getFridayNotificationMessage(tol, ttg);
			
			Notification notification = new Notification(subject, body);
			
			genericDAO.insertObject(notification);
			
			for(UserInfo user : userList){
				String hashCode = Utils.getHash(user.getFullName()+" ["+user.getMobileNumber()+"]", "sha1");
				StringBuffer formBuffer = Utils.getFormBodyString(notification.getId(), user.getFullName()+" ["+user.getMobileNumber()+"]", hashCode);
				StringBuffer signBuffer = Utils.getSignature(userInfo.getFullName(), userInfo.getMobileNumber());
				htmlMessage = body.concat(formBuffer.toString()).concat(signBuffer.toString());
				System.out.println(htmlMessage+"\n\n");
				isSuccess = mailer.sendEmail(user.getEmailId(), "", "", userInfo.getEmailId(), subject, htmlMessage, null, "");
			}
		}

		if(isSuccess){
			mav.addObject("msgType", "Success");
			mav.addObject("msg", "Notifcation sent successfully");
		} else {
			mav.addObject("msgType", "Error");
			mav.addObject("msg", "Error while sending notification");
		}

		mav.setViewName("redirect:/admin/onLoginSuccess.htm");
		return mav;	
	}

	@RequestMapping(value="/admin/onManageUsers.htm")
	public ModelAndView onManageUsers(HttpServletRequest request){
		initUserInfo(request);
		mav.addObject("userList", userDao.getAllUsers());
		mav.setViewName("manageUsers");
		return mav;	
	}

	@RequestMapping(value="/admin/enableAccount.htm")
	public ModelAndView enableAccount(HttpServletRequest request, HttpServletResponse response){
		initUserInfo(request);
		UserInfo userInfo = userDao.getUserInfoByEmail(request.getParameter("emailId").trim());

		if(null != userInfo){
			userInfo.setEnabled(!userInfo.getEnabled());
			genericDAO.updateObject(userInfo);
		}

		mav.setViewName("redirect:/admin/onManageUsers.htm");
		return mav;
	}

	@RequestMapping(value="/admin/deleteAccount.htm")
	public ModelAndView deleteAccount(HttpServletRequest request, HttpServletResponse response){
		initUserInfo(request);
		UserInfo userInfo = userDao.getUserInfoByEmail(request.getParameter("emailId").trim());

		if(null != userInfo){
			genericDAO.deleteObject(userInfo);
		}

		mav.setViewName("redirect:/admin/onManageUsers.htm");
		return mav;
	}

	@RequestMapping(value="/admin/clearRecords.htm")
	public ModelAndView clearRecords(HttpServletRequest request, HttpServletResponse response){
		initUserInfo(request);
		boolean bool = recordDAO.removeAllRecord();

		if(bool){
			mav.addObject("msgType", "Success");
			mav.addObject("msg", "Records cleared");
		}else{
			mav.addObject("msgType", "Error");
			mav.addObject("msg", "Error while clear records");
		}
		mav.setViewName("redirect:/admin/onLoginSuccess.htm");
		return mav;
	}

	@RequestMapping(value="/admin/toTransferAuthority.htm")
	public ModelAndView toTransferAuthority(HttpServletRequest request, HttpServletResponse response){
		UserInfo userInfo = initUserInfo(request);
		String oldAdminEmailId = userInfo.getEmailId();
		String newAdminName = request.getParameter("name");
		String newAdminEmailId = request.getParameter("emailId");
		
		String newPassword = newAdminEmailId;
		boolean bool = userDao.transferAuthority(newAdminEmailId, oldAdminEmailId, newPassword );
		
		if(bool){
			String message = Utils.getMessage("Congrats now you are Admin", newAdminEmailId, newPassword);
			mailer.sendEmail(newAdminEmailId, "", "", "", "Friday Namaz | Group Admin Changed", message, null, "");
			
			List<UserInfo> userList = userDao.getAllUsers();
			message = Utils.getMessage("We have a new group admin from today", newAdminName+" ("+newAdminEmailId+")", null);
			mailer.sendEmail(userList, "", "", "", "Friday Namaz | Group Admin Changed", message, null, "");
			
			mav.addObject("msgType", "Success");
			mav.addObject("msg", "Authority transferred");
			mav.setViewName("redirect:/j_spring_security_logout");
		}else{
			mav.addObject("msgType", "Error");
			mav.addObject("msg", "Error while Authority transfer");
			mav.setViewName("redirect:/admin/onLoginSuccess.htm");
		}
		return mav;
	}

	@RequestMapping(value="/admin/editAccount.htm")
	public ModelAndView editAccount(HttpServletRequest request, HttpServletResponse response){
		initUserInfo(request);
		
		UserInfo userInfo = userDao.getUserInfoByEmail(request.getParameter("emailId"));
		
		request.setAttribute("mode", "EDIT_PROFILE");
		request.setAttribute("editMode", "MM");
		request.setAttribute("USER_INFO", userInfo);
		mav.addObject("msg", "");
		mav.setViewName("addEditUser");
		
		return mav;
	}
	
	@PostConstruct
	public void addAdminUserIfNotExist(){
		if(userDao.getAllActiveUsers().isEmpty()){
			UserInfo userInfo = new UserInfo();
			userInfo.setAdminAccess(true);
			userInfo.setFullName("Mohd Amir");
			userInfo.setEmailId("mamir2@sapient.com");
			userInfo.setEnabled(true);
			userInfo.setMobileNumber("7531079343");
			userInfo.setPassword("default");

			genericDAO.insertObject(userInfo);
		}
	}
}