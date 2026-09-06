package com.hm.module.homemaking.service;

import com.hm.framework.security.core.LoginUser;
import com.hm.framework.tenant.core.context.TenantContextHolder;
import com.hm.module.homemaking.security.AdminScope;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringJUnitConfig(BusinessIsolationTest.Config.class)
class ReviewFlowTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired DataSource dataSource;
    @Autowired OrderService orders;
    @Autowired ReviewService reviews;
    @Autowired EvidenceStorage storage;
    long order;

    @BeforeEach void seed() {
        var fixture=new BusinessIsolationTest();fixture.jdbc=jdbc;fixture.dataSource=dataSource;fixture.seed();
        reset(storage);when(storage.save(anyLong(),any(byte[].class))).thenReturn(new EvidenceStorage.Saved("1/review.jpg","image/jpeg"));when(storage.read(anyString())).thenReturn(new byte[]{1,2,3});
        customer(1,1);order=orders.book(new OrderService.Book(1L,1L,LocalDate.now().plusDays(2).atTime(9,0),1L,"review-order"));
        jdbc.update("UPDATE hm_order SET status='COMPLETED',fulfillment_status='COMPLETED',completed_at=CURRENT_TIMESTAMP WHERE id=?",order);
    }
    @AfterEach void clear(){AdminScope.set(null);SecurityContextHolder.clearContext();TenantContextHolder.clear();}
    void customer(long tenant,long id){AdminScope.set(null);TenantContextHolder.setTenantId(tenant);TenantContextHolder.setIgnore(false);var user=new LoginUser();user.setId(id);user.setTenantId(tenant);user.setUserType(1);SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user,null,List.of()));}
    void admin(Set<Long> stores){TenantContextHolder.setTenantId(1L);TenantContextHolder.setIgnore(false);var user=new LoginUser();user.setId(42L);user.setTenantId(1L);user.setUserType(2);SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user,null,List.of()));AdminScope.set(new AdminScope(1,false,stores==null?"TENANT":"STORES",stores==null?Set.of():stores,null));}
    long draft(){return reviews.draft(new ReviewService.Draft(order,5,4,List.of("准时","专业","准时"),"服务细致，沟通清楚"));}

    @Test void draftStaysPrivateUntilEveryImageIsUploadedAndPublished(){
        long id=draft();assertTrue(orders.detail(order,false).get("review") instanceof List<?>);assertTrue(((List<?>)orders.detail(order,false).get("review")).isEmpty());
        long first=reviews.upload(id,"image-1",new byte[]{9});assertEquals(first,reviews.upload(id,"image-1",new byte[]{8}));assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM hm_review_image WHERE review_id=?",Integer.class,id));verify(storage,times(1)).save(eq(1L),any(byte[].class));
        var published=reviews.publish(id);assertEquals("PUBLISHED",published.get("status"));assertEquals(5,((Number)published.get("service_rating")).intValue());assertEquals(4,((Number)published.get("worker_rating")).intValue());assertEquals(List.of("准时","专业"),published.get("tags"));assertEquals(1,((List<?>)orders.detail(order,false).get("review")).size());
        assertThrows(Exception.class,()->reviews.publish(id));assertThrows(Exception.class,()->reviews.upload(id,"late",new byte[]{1}));assertThrows(Exception.class,()->draft());
        assertEquals(List.of("DRAFT_SAVED","IMAGE_ADDED","PUBLISHED"),jdbc.queryForList("SELECT action FROM hm_review_log WHERE review_id=? ORDER BY id",String.class,id));
    }

    @Test void retryingDraftReplacesPrivateImagesWithoutPublishingHalfAReview(){
        long id=draft();reviews.upload(id,"old",new byte[]{1});reviews.draft(new ReviewService.Draft(order,3,2,List.of("需改进"),"第二次提交内容"));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM hm_review_image WHERE review_id=?",Integer.class,id));verify(storage).deleteAfterCommit(List.of("1/review.jpg"));assertEquals("DRAFT",jdbc.queryForObject("SELECT status FROM hm_review WHERE id=?",String.class,id));assertFalse(jdbc.queryForObject("SELECT visible FROM hm_review WHERE id=?",Boolean.class,id));
    }

    @Test void imageLimitAndOwnershipAreEnforced(){
        long id=draft();for(int i=0;i<6;i++)reviews.upload(id,"image-"+i,new byte[]{1});assertThrows(Exception.class,()->reviews.upload(id,"image-7",new byte[]{1}));
        customer(1,2);assertThrows(Exception.class,()->reviews.customerDetail(id));assertThrows(Exception.class,()->reviews.upload(id,"foreign",new byte[]{1}));assertThrows(Exception.class,()->reviews.image(id,1,false));
        customer(2,2);assertThrows(Exception.class,()->reviews.customerDetail(id));
    }

    @Test void merchantReplyModerationAndPrivateImageReadAreAudited(){
        long id=draft();long image=reviews.upload(id,"image",new byte[]{1});reviews.publish(id);admin(null);
        reviews.reply(id,new ReviewService.Reply("感谢认可，欢迎再次预约"));assertThrows(Exception.class,()->reviews.moderate(id,new ReviewService.Moderation(false,true)));reviews.moderate(id,new ReviewService.Moderation(true,true));
        var detail=reviews.adminDetail(id);assertEquals("感谢认可，欢迎再次预约",detail.get("reply_content"));assertEquals(true,detail.get("visible"));assertEquals(true,detail.get("recommended"));assertEquals(3,reviews.image(id,image,true).getBody().length);assertEquals(5,((List<?>)detail.get("logs")).size());
        reviews.moderate(id,new ReviewService.Moderation(false,false));assertFalse(jdbc.queryForObject("SELECT visible FROM hm_review WHERE id=?",Boolean.class,id));assertFalse(jdbc.queryForObject("SELECT recommended FROM hm_review WHERE id=?",Boolean.class,id));
    }

    @Test void storeDataScopeFiltersListDetailAndImages(){
        long id=draft();long image=reviews.upload(id,"image",new byte[]{1});reviews.publish(id);admin(Set.of(2L));assertTrue(reviews.adminList().isEmpty());assertThrows(Exception.class,()->reviews.adminDetail(id));assertThrows(Exception.class,()->reviews.image(id,image,true));
        admin(Set.of(1L));assertEquals(1,reviews.adminList().size());assertDoesNotThrow(()->reviews.adminDetail(id));
    }

    @Test void hiddenAndDraftReviewsNeverEnterPublicMiniappListing(){
        long id=draft();var mini=new com.hm.module.homemaking.controller.app.LegacyMiniController(new com.hm.module.homemaking.dal.HmRepository(jdbc),orders,null,null,new com.fasterxml.jackson.databind.ObjectMapper());
        assertTrue(((List<?>)((Map<?,?>)mini.home().getData()).get("reviewList")).isEmpty());reviews.publish(id);assertTrue(((List<?>)((Map<?,?>)mini.home().getData()).get("reviewList")).isEmpty());admin(null);reviews.moderate(id,new ReviewService.Moderation(true,true));customer(1,1);assertEquals(1,((List<?>)((Map<?,?>)mini.home().getData()).get("reviewList")).size());
    }
}
