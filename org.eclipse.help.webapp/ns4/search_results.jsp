<%@ page import="java.net.URLEncoder,java.text.NumberFormat,org.eclipse.help.servlet.*,org.w3c.dom.*" errorPage="err.jsp" contentType="text/html; charset=UTF-8"%>

<% 
	// calls the utility class to initialize the application
	application.getRequestDispatcher("/servlet/org.eclipse.help.servlet.InitServlet").include(request,response);
%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<!--
 (c) Copyright IBM Corp. 2000, 2002.
 All Rights Reserved.
-->
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<meta http-equiv="Pragma" content="no-cache">
<meta http-equiv="Expires" content="-1">
<base target="MainFrame">
<!--
<script language="JavaScript" src="list.js"></script>
-->
<script language="JavaScript">		
function refresh() 
{ 
	window.location.replace("search_results.jsp?<%=request.getQueryString()%>");
}
</script>

<style type="text/css">
BODY {
	font: 8pt Tahoma;
}


A {
	text-decoration:none; 
	color:black; 
	padding:0px;
	white-space: nowrap;
	cursor:default;
}


TABLE, TD {
	font: 8pt Tahoma;
}


</style>

</head>

<body BGCOLOR="#FFFFFF">

<%
if(request.getParameter("searchWord")!=null || request.getParameter("searchWordJS13")!=null)
{
	// Load the results
	ContentUtil content = new ContentUtil(application, request);
	String sQuery=request.getQueryString();
	sQuery=UrlUtil.changeParameterEncoding(sQuery, "searchWordJS13", "searchWord");
	sQuery=UrlUtil.changeParameterEncoding(sQuery, "scopeJS13", "scope");
	Element resultsElement = content.loadSearchResults(sQuery);
	if (resultsElement == null){
		out.write(WebappResources.getString("Nothing_found", request));
		return;
	}
	
	// If we did not receive a <toc> then display a progress monitor
	if (!resultsElement.getTagName().equals("toc"))
	{
		String percentage = resultsElement.getAttribute("indexed");
%>

<CENTER>
<TABLE BORDER='0'>
	<TR><TD><%=WebappResources.getString("Indexing", request)%></TD></TR>
	<TR><TD ALIGN='LEFT'>
		<DIV STYLE='width:100px;height:14px;border:1px solid black;'>
			<IMG src="../images/progress.gif" height="14" width="<%=percentage%>">
		</DIV>
	</TD></TR>
	<TR><TD><%=percentage%>% <%=WebappResources.getString("complete", request)%></TD></TR>
</TABLE>
</CENTER>
<script language='JavaScript'>
setTimeout('refresh()', 2000);
</script>
</body>
</html>

<%
		return;
	}
		
	// Generate results list
	NodeList topics = resultsElement.getElementsByTagName("topic");
	if (topics == null || topics.getLength() == 0){
		out.write(WebappResources.getString("Nothing_found", request));
		return;
	}
%>

<table id='list'  cellspacing='0' >

<%
	for (int i = 0; i < topics.getLength(); i++) 
	{
		Element topic = (Element)topics.item(i);
		// obtain document score
		String scoreString = topic.getAttribute("score");
		try {
			float score = Float.parseFloat(scoreString);
			NumberFormat percentFormat =
				NumberFormat.getPercentInstance(request.getLocale());
			scoreString = percentFormat.format(score);
		} catch (NumberFormatException nfe) {
			// will display original score string
		}

		String tocLabel = topic.getAttribute("toclabel");
		String label = topic.getAttribute("label");
		String href = topic.getAttribute("href");
		if (href != null && href.length() > 0) {
			// external href
			if (href.charAt(0) == '/')
				href = "content/help:" + href;
			if (href.indexOf('?') == -1)
				href +="?toc="+URLEncoder.encode(topic.getAttribute("toc"));
			else
				href += "&toc="+URLEncoder.encode(topic.getAttribute("toc"));			

		} else
			href = "javascript:void 0";
%>

<tr class='list'>
	<td class='score' align='right'><%=scoreString%>&nbsp;</td>
	<td align='left' class='label' nowrap>
		<a href='<%=href%>' onclick='parent.parent.setToolbarTitle("<%=UrlUtil.JavaScriptEncode(tocLabel)%>")'><%=label%></a>
	</td>
</tr>

<%
	}
%>

</table>

<%
}else{
	out.write("<table><tr><td><p style='padding-left:5px; padding-right:15px;'>");
	out.write(WebappResources.getString("doSearch", request));
	out.write("</p></td></tr></table>");
}

%>


</body>
</html>

