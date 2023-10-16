package com.idukbaduk.metoo9dan.payments.controller;

import com.idukbaduk.metoo9dan.common.entity.*;
import com.idukbaduk.metoo9dan.game.service.GameFilesService;
import com.idukbaduk.metoo9dan.game.service.GameService;
import com.idukbaduk.metoo9dan.member.service.MemberService;
import com.idukbaduk.metoo9dan.payments.kakaopay.KakaoApproveResponse;
import com.idukbaduk.metoo9dan.payments.kakaopay.KakaoPayService;
import com.idukbaduk.metoo9dan.payments.kakaopay.KakaoReadyResponse;
import com.idukbaduk.metoo9dan.payments.service.PaymentsService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentsController {

    private final GameService gameService;
    private final GameFilesService gameFilesService;
    private final KakaoPayService kakaoPayService;
    private final PaymentsService paymentsService;
    private final MemberService memberService;

    // 결제하기 폼
    @PostMapping("/paymentsform")
    public String paymentsform(@RequestParam(value = "gameContentNo", required = false) List<Integer> gameContentNo, Model model, HttpSession session) {
        if (gameContentNo != null && !gameContentNo.isEmpty()) {
            List<GameContents> selectedGameContents = new ArrayList<>();

            int totalSalePrice = 0;

            for (int gameno : gameContentNo) {
                GameContents gameContents = gameService.getGameContents(gameno);
                selectedGameContents.add(gameContents);
                // salePrice의 합계 계산
                totalSalePrice += (gameContents.getSalePrice());
                System.out.println("totalSalePrice?" +totalSalePrice);
            }

            // 세션에 선택한 게임 컨텐츠와 총 판매 가격 저장
            session.setAttribute("selectedGameContents", selectedGameContents);
            session.setAttribute("totalSalePrice", totalSalePrice);

            /*model.addAttribute("selectedGameContents", selectedGameContents);
            model.addAttribute("totalSalePrice", totalSalePrice);*/
        }
        return "payments/paymentsform";
    }


    // 결제하기
    @PostMapping("/payments")
    public String processPayment(@RequestParam(value = "paymentMethod") String paymentMethod,HttpSession session) {

        List<GameContents> selectedGameContents = (List<GameContents>) session.getAttribute("selectedGameContents");
        System.out.println("selectedGameContents?" + selectedGameContents);
        int totalSalePrice = (int) session.getAttribute("totalSalePrice");
        System.out.println("totalSalePrice:?" +totalSalePrice);
        //임시용 ---------- 추후 수정 필 -------------
        String memberID = "lee123";
        Member user = memberService.getUser(memberID);

        // paymentMethod 변수에 선택한 결제 방법이 무통장이면
        if(paymentMethod.equals("deposit")){
            paymentsService.save(selectedGameContents,user,paymentMethod);
        }

        return "payments/list";
    }


    //게임컨텐츠 구매 목록조회
    @GetMapping("/list")
    public String gameList(Model model, @RequestParam(value = "page", defaultValue = "0") int page, GameContents gameContents) {

        //임시용 ---------- 추후 수정 필 -------------
        String memberID = "lee123";
        Member user = memberService.getUser(memberID);

        List<Payments> payments = paymentsService.paymentsList(user.getMemberNo());


        for (Payments payments1 : payments) {
            payments1.getGameContents()
        }

        GameContents gameContents1 = gameService.getGameContents(payments.get(0).);
        // 게임컨텐츠 목록 조회
        Page<GameContents> gamePage = this.gameService.getList(page);

        for (GameContents gamecon : gamePage.getContent()) {
            // 게임컨텐츠에 대한 파일 정보 가져오기
            List<GameContentFiles> gameContentFilesList = gameFilesService.getGameFilesByGameContentNo(gamecon.getGameContentNo());
            gamecon.setGameContentFilesList(gameContentFilesList);

            // 게임컨텐츠에 대한 교육자료 정보 가져오기
            List<EducationalResources> education = educationService.getEducation_togameno(gamecon.getGameContentNo());

            for (EducationalResources educationalResource : education) {
                List<ResourcesFiles> resourcesFilesByResourceNo = resourcesFilesService.getResourcesFilesByResourceNo(educationalResource.getResourceNo());
                educationalResource.setResourcesFilesList(resourcesFilesByResourceNo);
            }
            gamecon.setEducationalResourcesList(education);

        }
        model.addAttribute("gamePage", gamePage);




        model.addAttribute("payments",payments);

        return "payments/list";
    }


    // 결제 성공시
    @GetMapping("/success")
    public String afterPayRequest(@RequestParam("pg_token") String pgToken, Model model) {
        System.out.println("afterPayRequest? " + pgToken);
        KakaoApproveResponse kakaoApprove = kakaoPayService.approveResponse(pgToken);

        // 여기에서 game/list.html로 리다이렉트
        return "redirect:/game/list";
    }

    //결제 취소 할때 보여주는 페이지
    @GetMapping("/cancel")
    public String paymentscancel() {
        return "payments/cancel";
    }

    //결제실패 할때 보여주는 페이지
    @GetMapping("/fail")
    public String paymentsfail() {
        return "payments/fail";
    }

}
